package com.mpc.experinment_8

import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class BluetoothDeviceAdapter(
    private val onDeviceClick: ((BluetoothDeviceItem) -> Unit)? = null,
    private val onConnectClick: ((BluetoothDeviceItem) -> Unit)? = null
) : RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder>() {
    
    private val devices = mutableListOf<BluetoothDeviceItem>()
    
    companion object {
        private const val TAG = "BluetoothDeviceAdapter"
    }
    
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: MaterialCardView = view.findViewById(R.id.cardView)
        val deviceTypeIcon: ImageView = view.findViewById(R.id.ivDeviceTypeIcon)
        val deviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val deviceType: TextView = view.findViewById(R.id.tvDeviceType)
        val deviceAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
        val signalStrengthContainer: LinearLayout = view.findViewById(R.id.signalStrengthContainer)
        val signalIcon: ImageView = view.findViewById(R.id.ivSignalIcon)
        val signalStrength: TextView = view.findViewById(R.id.tvSignalStrength)
        val bondIcon: ImageView = view.findViewById(R.id.ivBondIcon)
        val bondStatus: TextView = view.findViewById(R.id.tvBondStatus)
        val rssiIcon: ImageView = view.findViewById(R.id.ivRssiIcon)
        val rssiValue: TextView = view.findViewById(R.id.tvRssiValue)
        val discoveryTime: TextView = view.findViewById(R.id.tvDiscoveryTime)
        val connectButton: MaterialButton = view.findViewById(R.id.btnConnect)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bluetooth_device, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val device = devices[position]
            val context = holder.itemView.context
            
            // Bind basic device information
            holder.deviceName.text = device.getDisplayName()
            holder.deviceType.text = device.getDeviceTypeString()
            holder.deviceAddress.text = device.address
            holder.discoveryTime.text = "Discovered ${device.getFormattedDiscoveryTime()}"
            
            // Set device type icon and color with fallback
            holder.deviceTypeIcon.setImageResource(device.getDeviceTypeIcon())
            val deviceColor = device.getDeviceTypeColor(context)
            holder.deviceTypeIcon.setColorFilter(deviceColor)
            
            // Set signal strength information with fallback
            holder.signalIcon.setImageResource(device.getSignalStrengthIcon())
            holder.signalStrength.text = device.getSignalStrengthString()
            val signalColor = device.getSignalStrengthColor(context)
            
            // Update signal strength container background with fallback
            try {
                val signalBackground = holder.signalStrengthContainer.background as? GradientDrawable
                signalBackground?.setColor(signalColor)
                holder.signalIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.white))
            } catch (e: Exception) {
                Log.w(TAG, "Could not set signal strength background: ${e.message}")
                // Use a default color if the drawable can't be modified
                holder.signalStrengthContainer.setBackgroundColor(signalColor)
            }
            
            // Set bond status
            holder.bondIcon.setImageResource(device.getBondStateIcon())
            holder.bondStatus.text = device.getBondStateString()
            
            // Set RSSI information
            holder.rssiValue.text = device.getRssiDisplayString()
            
            // Handle connect button visibility and click
            if (onConnectClick != null && device.bondState != 10) { // Not bonded
                holder.connectButton.visibility = View.VISIBLE
                holder.connectButton.setOnClickListener {
                    onConnectClick.invoke(device)
                }
            } else {
                holder.connectButton.visibility = View.GONE
            }
            
            // Handle card click
            holder.cardView.setOnClickListener {
                onDeviceClick?.invoke(device)
                animateCardPress(holder.cardView)
            }
            
            // Add subtle highlight for recently discovered devices with fallback
            try {
                if (device.isRecent()) {
                    val secondaryColor = ContextCompat.getColor(context, R.color.secondary)
                    holder.cardView.strokeColor = secondaryColor
                    holder.cardView.strokeWidth = 3
                } else {
                    val cardStrokeColor = ContextCompat.getColor(context, R.color.card_stroke)
                    holder.cardView.strokeColor = cardStrokeColor
                    holder.cardView.strokeWidth = 1
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set card stroke color: ${e.message}")
                // Use default colors if custom colors are not available
                if (device.isRecent()) {
                    holder.cardView.strokeColor = ContextCompat.getColor(context, android.R.color.holo_blue_light)
                    holder.cardView.strokeWidth = 3
                } else {
                    holder.cardView.strokeColor = ContextCompat.getColor(context, android.R.color.darker_gray)
                    holder.cardView.strokeWidth = 1
                }
            }
            
            // Animate card entry
            animateCardEntry(holder.cardView, position)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error binding device at position $position: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun getItemCount(): Int = devices.size
    
    fun addDevice(device: BluetoothDeviceItem) {
        try {
            val existingIndex = devices.indexOfFirst { it.address == device.address }
            if (existingIndex != -1) {
                // Update existing device with new information (e.g., updated RSSI)
                devices[existingIndex] = device
                notifyItemChanged(existingIndex)
                Log.d(TAG, "Updated existing device: ${device.getDisplayName()}")
            } else {
                // Add new device
                val position = devices.size
                devices.add(device)
                notifyItemInserted(position)
                Log.d(TAG, "Added new device: ${device.getDisplayName()} at position $position")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding device: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun clearDevices() {
        try {
            val size = devices.size
            devices.clear()
            notifyItemRangeRemoved(0, size)
            Log.d(TAG, "Cleared $size devices from adapter")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing devices: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun getDevices(): List<BluetoothDeviceItem> = devices.toList()
    
    fun sortDevicesBySignalStrength() {
        try {
            devices.sortByDescending { it.rssi }
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error sorting by signal strength: ${e.message}")
        }
    }
    
    fun sortDevicesByName() {
        try {
            devices.sortBy { it.getDisplayName().lowercase() }
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error sorting by name: ${e.message}")
        }
    }
    
    fun sortDevicesByDiscoveryTime() {
        try {
            devices.sortByDescending { it.discoveredTime }
            notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error sorting by discovery time: ${e.message}")
        }
    }
    
    fun filterDevicesByType(deviceType: Int?) {
        // This would require implementing filtering logic
        // For now, it's a placeholder for future enhancement
    }
    
    private fun animateCardEntry(cardView: MaterialCardView, position: Int) {
        try {
            cardView.alpha = 0f
            cardView.translationY = 50f
            cardView.scaleX = 0.9f
            cardView.scaleY = 0.9f
            
            cardView.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay((position % 3) * 100L) // Staggered animation
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        } catch (e: Exception) {
            Log.w(TAG, "Error animating card entry: ${e.message}")
            // Fallback: just make the card visible
            cardView.alpha = 1f
            cardView.translationY = 0f
            cardView.scaleX = 1f
            cardView.scaleY = 1f
        }
    }
    
    private fun animateCardPress(cardView: MaterialCardView) {
        try {
            val scaleDown = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 0.95f)
            scaleDown.duration = 100
            scaleDown.interpolator = AccelerateDecelerateInterpolator()
            
            val scaleDownY = ObjectAnimator.ofFloat(cardView, "scaleY", 1f, 0.95f)
            scaleDownY.duration = 100
            scaleDownY.interpolator = AccelerateDecelerateInterpolator()
            
            scaleDown.start()
            scaleDownY.start()
            
            cardView.postDelayed({
                val scaleUp = ObjectAnimator.ofFloat(cardView, "scaleX", 0.95f, 1f)
                scaleUp.duration = 100
                scaleUp.interpolator = AccelerateDecelerateInterpolator()
                
                val scaleUpY = ObjectAnimator.ofFloat(cardView, "scaleY", 0.95f, 1f)
                scaleUpY.duration = 100
                scaleUpY.interpolator = AccelerateDecelerateInterpolator()
                
                scaleUp.start()
                scaleUpY.start()
            }, 100)
        } catch (e: Exception) {
            Log.w(TAG, "Error animating card press: ${e.message}")
        }
    }
}
