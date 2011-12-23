#!/bin/bash

# don't ask me no questions and I won't tell you no lies 
export DEBIAN_FRONTEND=noninteractive

# import PGP keys
gpg --keyserver  hkp://keys.gnupg.net --recv-keys 1C4CBDCDCD2EFD2A
gpg -a --export CD2EFD2A | apt-key add -

# set up the percona repo
echo "deb http://repo.percona.com/apt lucid main
deb-src http://repo.percona.com/apt lucid main" >/etc/apt/sources.list.d/percona.list

apt-get update
apt-get --force-yes --yes install percona-server-common-5.5  percona-server-server-5.5 percona-server-test-5.5  percona-server-client-5.5 libmysqlclient18  libmysqlclient-dev xtrabackup

# change root password
/usr/bin/mysqladmin -u root password hpcs

# now shut down
/etc/init.d/mysql stop

# we will be moving this to a new mount point
mv /var/lib/mysql /var/lib/mysql.bak

# copy stock my.cnf
echo "[mysqld]
user=mysql

datadir=/var/lib/mysql

innodb_buffer_pool_size=2G
innodb_log_file_size=100M
innodb_file_per_table 
" > /etc/mysql/my.cnf

# remove innodb log file since log file size is changed
rm /var/lib/mysql.bak/ib_logfile*

# partition, set as LVM
# this WILL be dynamic/generated depending on what hosts the user wants to allow, for now we open wide
fdisk /dev/vdb <<EOF
n
p
1


t
8e
w
EOF

# install lvm
apt-get -y install lvm2

# creat physical volume
pvcreate /dev/vdb1

# create volume group
vgcreate data /dev/vdb1

# create logical volume - this needs to be changed to account for flavor (size)
lvcreate --size 100G --name mysql-data data

# install xfs utils
apt-get -y install xfsprogs xfsdump

# format
mkfs.xfs /dev/data/mysql-data

# create mount point
mkdir /var/lib/mysql

# change ownership
chown mysql:mysql /var/lib/mysql

# get mount point into fstab
echo -e "\n/dev/data/mysql-data\t/var/lib/mysql\txfs\tdefaults\t0\t0\n" >> /etc/fstab

# mount
mount -a 
 
# copy data
cp -a /var/lib/mysql.bak/* /var/lib/mysql/

# ensure ownership
chown -R mysql:mysql /var/lib/mysql

# start
/etc/init.d/mysql start

# this WILL be dynamic
/usr/bin/mysql -u root -phpcs -e "grant all privileges on *.* to 'root'@'%' identified by 'hpcs' with grant option;"

# $1 metadata key
function ec2_meta() {
  curl -s "http://169.254.169.254/latest/meta-data/$1"
}
PUBLIC_IP=$(ec2_meta public-ipv4)
INSTANCE_ID=$(ec2_meta instance-id)

# URL to post for success (solely for demo purposes) 
OAAS_URL=
curl -X POST $OAAS_URL/server/$INSTANCE_ID -d "{\"ip\": \"$PUBLIC_IP\", \"chefStatus\": \"mysql is up\"}"
