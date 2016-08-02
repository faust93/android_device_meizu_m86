#!/system/bin/sh
pnet=`settings list global | grep preferred_network_mode= | cut -d= -f2`

if [ -z "$pnet" ]; then
    pnet="1,9"
fi

id0=`echo $pnet | cut -d, -f1`
id1=`echo $pnet | cut -d, -f2`

am broadcast -a com.android.internal.telephony.MODIFY_NETWORK_MODE --ei networkMode $id0 --ei subId 0
am broadcast -a com.android.internal.telephony.MODIFY_NETWORK_MODE --ei networkMode $id1 --ei subId 1
