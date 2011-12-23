#!/bin/bash
#
# Installs riak (but doesn't yet get as far as configuring it)
#
# This is the "I haven't bothered to learn chef" approach
#
# Intended to be run within an hpcloud instance by cloud-init as root


# quit on first error
set -e

WORKBENCH="/tmp/workbench"
LOGFILE="/tmp/bootstrap.txt"
# XXX: cloud-init runs this without HOME set, probably for good reason,
#      but this makes erlc unhappy; fake it and see if we can get by 
export HOME="/root"

# $1 metadata key
function ec2_meta() {
  curl -s "http://169.254.169.254/latest/meta-data/$1"
}

PRIVATE_IP=$(ec2_meta local-ipv4)
PUBLIC_IP=$(ec2_meta public-ipv4)
INSTANCE_ID=$(ec2_meta instance-id)

function hackish_log() {
  # XXX: since I can't figure out where (if?) cloud-init redirects stdout/stderr
  echo `date`" - hackish: $@" | tee -a $LOGFILE
}

function preamble() {
  hackish_log "I'm running as "$(whoami)
  hackish_log "My private ip is $PRIVATE_IP"
  hackish_log "My public ip is $PUBLIC_IP"
  hackish_log "My home is $HOME"
}

function slam_dns_to_google() {
  # XXX: use google's dns since my AZ2 default of 10.6.221.1 isn't working
  echo "nameserver 8.8.8.8" >> /etc/resolv.conf
  chmod 444 /etc/resolv.conf
  # XXX: kill dhclient so it stops stomping over resolv.conf
  for dhcpid in $(ps aux | grep dhclient | grep -v grep | awk '{print $2}'); do
    kill -9 $dhcpid;
  done
}

function install_git() {
  cd $WORKBENCH
  # install git
  hackish_log "installing git..."
  apt-get -y install git | tee -a $LOGFILE
}

function install_erlang_from_ppa() {
  cd $WORKBENCH
  # It Works For Me (TM) debian testing backports
  hackish_log "installing erlang from ppa..."
  add-apt-repository -y ppa:scattino/ppa 2>&1 | tee -a $LOGFILE
  apt-get update 2>&1 | tee -a $LOGFILE
  apt-get install -y erlang 2>&1 |  tee -a $LOGFILE
}

function install_riak() {
  cd $WORKBENCH
  # install riak
  hackish_log "installing riak..."
  # workaround (see http://www.mail-archive.com/riak-users@lists.basho.com/msg05487.html)
  apt-get -y install libssl0.9.8 | tee -a $LOGFILE
  # sanity check that dns isn't about to fail us
  ping -c1 downloads.basho.com 2>&1 | tee -a $LOGFILE
  wget http://downloads.basho.com/riak/riak-1.0.2/riak_1.0.2-1_amd64.deb
  dpkg -i riak_1.0.2-1_amd64.deb 2>&1 | tee -a $LOGFILE
}

function install_basho_bench() {
  cd $WORKBENCH
  apt-get -y install build-essential libc6-dev-i386
  git clone git://github.com/basho/basho_bench
  cd basho_bench
  make
}

function install_r() {
  cd $WORKBENCH
  # install R
  hackish_log "installing r..."
  apt-get -y install r-base r-base-dev
}

function setup_vdb_for_bitcask() {
  cd $WORKBENCH
  hackish_log "allocating all of vdb..."
  parted -s /dev/vdb mklabel gpt 2>&1 | tee -a $LOGFILE
  parted -s /dev/vdb -a cylinder mkpart primary 0 -- -1 2>&1 | tee -a $LOGFILE
  mkfs -t ext3 /dev/vdb1 2>&1 | tee -a $LOGFILE
  echo -e "\n/dev/vdb1\t/var/lib/riak/bitcask\text3 defaults,noatime\t0\t0\n" >> /etc/fstab
  mount -a 2>&1 | tee -a $LOGFILE
  chown riak:riak /var/lib/riak/bitcask 2>&1 | tee -a $LOGFILE
}

function turn_off_byobu() {
  su ubuntu -c byobu-disable
}

function setup() {
  touch $LOGFILE
  hackish_log "setting up..."
  slam_dns_to_google
  apt-get update 2>&1 | tee -a $LOGFILE
  mkdir -p $WORKBENCH
  turn_off_byobu
}

function install() {
  install_git
  install_erlang_from_ppa
  install_riak
  setup_vdb_for_bitcask
  install_basho_bench
  # install_r
}

function configure_riak() {
  hackish_log "configuring riak"
  # change the default name to use our private ip
  sed -i'' -e "s|^-name.*|-name riak@$PRIVATE_IP|" /etc/riak/vm.args
  # bind to our private ip's
  sed -i'' -e "s|127\.0\.0\.1|0.0.0.0|g" /etc/riak/app.config

}

function configure() {
  configure_riak
}

# URL to post for success (solely for demo purposes) 
OAAS_URL=
function signal_success() {
  hackish_log "posting for great justice"
  curl -X POST $OAAS_URL/server/$INSTANCE_ID -d "{\"ip\": \"$PUBLIC_IP\", \"chefStatus\": \"riak is up\"}" 2>&1 | tee -a $LOGFILE
}

preamble

setup

install

configure

# run riak
hackish_log "running riak..."
riak start

# signal success
touch /tmp/riak-bootstrap-done
hackish_log "riak bootstrap done"
signal_success
hackish_log "riak bootstrap really done"

