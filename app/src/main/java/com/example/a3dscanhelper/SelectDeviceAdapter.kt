package com.example.a3dscanhelper

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class SelectDeviceAdapter (private val context: Context,
                           private val dataSource: ArrayList<BluetoothDevice>) : BaseAdapter() {

    private val inflater: LayoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {return dataSource.size}
    override fun getItem(position: Int): Any {return dataSource[position]}
    override fun getItemId(position: Int): Long {return position.toLong()}

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = inflater.inflate(R.layout.selectdevice_listitem_layout, parent, false)

        val deviceNameTextView=rowView.findViewById(R.id.BTDeviceName) as TextView
        val deviceAddressTextView=rowView.findViewById(R.id.BTDeviceAddress) as TextView

        val actualDevice=getItem(position) as BluetoothDevice

        deviceNameTextView.text=actualDevice.name
        deviceAddressTextView.text=actualDevice.address

        return rowView
    }
}