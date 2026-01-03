#!/bin/bash
echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDNp0+ksXQNpvRMeBXTvyJW99LTxQzi9jzwDbN+Y+ySaI+OqRx+uRl8gkwACKSTFkdunkE132M/gr1qdEZ1fo8rtFT219fnh4e4kGoNis08Yz4tp/qwaYKYBFdN+MtRIOuFZzbHRydFSkEkyZtEUbJyD2Yo8HauoCB8g18MpHGBpOY1wbnVsrmR+YmFkfzQqti0DX7KHG7H5imMGdKn4Qn4sxqK5OWy2NB0a2TtvjhCxJ9rVaeGhbH+AzmATn9Wy4zk9r/Tm3G8ktDf+6uEF3W2cdEmj3CNJKUX1851IlwE7ox8G+zdouvOD/Jcsp2CK1JJ0u1GnUe/Fek/e45l8Y5n indcloud-production-key' >> /home/ec2-user/.ssh/authorized_keys
chown ec2-user:ec2-user /home/ec2-user/.ssh/authorized_keys
chmod 600 /home/ec2-user/.ssh/authorized_keys
