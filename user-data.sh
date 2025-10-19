#!/bin/bash
  set -e
  apt-get update -y
  apt-get upgrade -y
  curl -fsSL https://get.docker.com -o get-docker.sh
  sh get-docker.sh
  curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
  chmod +x /usr/local/bin/docker-compose
  apt-get install -y awscli
  mkdir -p /home/ubuntu/sensorvision
  chown ubuntu:ubuntu /home/ubuntu/sensorvision
  usermod -aG docker ubuntu
