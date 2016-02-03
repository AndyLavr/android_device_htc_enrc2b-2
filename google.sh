#!/system/bin/sh
#
IPTABLES="/system/bin/iptables"
#
for DNS in `getprop redirect.dns`; do
# sleep 1
$IPTABLES -t nat -I OUTPUT -p tcp --dport 53 -j DNAT --to-destination $DNS:53
# sleep 1
$IPTABLES -t nat -I OUTPUT -p udp --dport 53 -j DNAT --to-destination $DNS:53
done
#
exit
