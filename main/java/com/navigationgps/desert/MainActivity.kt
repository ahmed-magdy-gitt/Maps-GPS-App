package com.navigationgps.desert

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.cardview.widget.CardView
import com.navigationgps.desert.databinding.ActivityMainBinding
import com.navigationgps.desert.databinding.ItemCategoriesBinding
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.view.animation.CycleInterpolator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val defaultCardColor = Color.parseColor("#92663E")
    private val PREF_NAME = "CompassPrefs"
    private val KEY_NOTIFICATION_SHOWN = "notification_shown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // ✅ تحديث التنويه حسب حالة البوصلة
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val updated = prefs.getBoolean(KEY_NOTIFICATION_SHOWN, false)

        if (!updated) {
            binding.compassNotification.text = "⚠️ تنويه: يجب تحديث البوصلة أولاً لضمان دقة البيانات!"
            binding.compassNotification.setBackgroundResource(R.drawable.notification_banner_bg)
            binding.compassNotification.setTextColor(Color.parseColor("#8B4513"))
            binding.compassNotification.visibility = View.VISIBLE
        } else {
            binding.compassNotification.text = "✅ البوصلة محدثة وجاهزة للاستخدام!"
            binding.compassNotification.setBackgroundResource(R.drawable.notification_banner_updated_bg)
            binding.compassNotification.setTextColor(Color.parseColor("#2E7D32"))
            binding.compassNotification.visibility = View.VISIBLE
        }

        //GridView
        val names = listOf("البوصلة", "المواقع", "المفضلة", "المسافة بين موقعين")
        val images = listOf(
            R.drawable.compass,
            R.drawable.locations,
            R.drawable.favorite,
            R.drawable.destination
        )

        val adapter = object : BaseAdapter() {
            override fun getCount(): Int = names.size
            override fun getItem(p0: Int): Any = names[p0]
            override fun getItemId(p0: Int): Long = p0.toLong()

            override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
                val itemBinding = ItemCategoriesBinding.inflate(layoutInflater, p2, false)
                itemBinding.itemTx.text = names[p0]
                itemBinding.itemImg.setImageResource(images[p0])

                val cardView = itemBinding.root as CardView
                cardView.setCardBackgroundColor(defaultCardColor)

                if (p0 == 0) {
                    val animator = ObjectAnimator.ofFloat(cardView, "translationY", 0f, -5f, 5f, 0f)
                    animator.duration = 800
                    animator.repeatCount = ObjectAnimator.INFINITE
                    animator.interpolator = CycleInterpolator(0.5f)
                    animator.start()
                    cardView.cardElevation = 15f
                } else {
                    cardView.translationY = 0f
                    ObjectAnimator.ofFloat(cardView, "translationY", 0f).cancel()
                    cardView.cardElevation = 15f
                }

                return cardView
            }
        }


        binding.GridView.adapter = adapter

        binding.GridView.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> {
                    val intent = Intent(this, CompassActivity::class.java)
                    startActivity(intent)
                }
                1 -> {
                    val intent = Intent(this, LocationActivity::class.java)
                    startActivity(intent)
                }

               2 -> {
                    val intent = Intent(this, FavoritesActivity::class.java)
                    startActivity(intent)
                }
               3 -> {
                    val intent = Intent(this, DistanceBtnActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        configuration.fontScale = 1.0f
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }

}
