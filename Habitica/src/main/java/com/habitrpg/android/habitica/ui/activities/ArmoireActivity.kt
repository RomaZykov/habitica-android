package com.habitrpg.android.habitica.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.habitrpg.android.habitica.R
import com.habitrpg.android.habitica.components.UserComponent
import com.habitrpg.android.habitica.data.InventoryRepository
import com.habitrpg.android.habitica.databinding.ActivityArmoireBinding
import com.habitrpg.android.habitica.extensions.observeOnce
import com.habitrpg.android.habitica.helpers.AdHandler
import com.habitrpg.android.habitica.helpers.AdType
import com.habitrpg.android.habitica.helpers.AppConfigManager
import com.habitrpg.android.habitica.helpers.RxErrorHandler
import com.habitrpg.android.habitica.ui.helpers.loadImage
import com.habitrpg.android.habitica.ui.viewmodels.MainUserViewModel
import com.habitrpg.android.habitica.ui.views.dialogs.HabiticaBottomSheetDialog
import com.plattysoft.leonids.ParticleSystem
import javax.inject.Inject

class ArmoireActivity: BaseActivity() {

    private var equipmentKey: String? = null
    private var gold: Double? = null
    private var hasAnimatedChanges: Boolean = false
    private lateinit var binding: ActivityArmoireBinding

    @Inject
    internal lateinit var inventoryRepository: InventoryRepository
    @Inject
    internal lateinit var appConfigManager: AppConfigManager
    @Inject
    lateinit var userViewModel: MainUserViewModel

    override fun getLayoutResId(): Int = R.layout.activity_armoire

    override fun injectActivity(component: UserComponent?) {
        component?.inject(this)
    }

    override fun getContentView(): View {
        binding = ActivityArmoireBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.goldView.currency = "gold"
        binding.goldView.animationDuration = 1000
        binding.goldView.animationDelay = 500
        binding.goldView.minForAbbrevation = 1000000
        binding.goldView.decimals = 0

        userViewModel.user.observeOnce(this) { user ->
            gold = user?.stats?.gp
            val remaining = inventoryRepository.getArmoireRemainingCount()
            binding.equipmentCountView.text = getString(R.string.equipment_remaining, remaining)
            binding.noEquipmentView.visibility = if (remaining > 0) View.GONE else View.VISIBLE
        }

        if (appConfigManager.enableArmoireAds()) {
            val handler = AdHandler(this, AdType.ARMOIRE) {
                Log.d("AdHandler", "Giving Armoire")
                val user = userViewModel.user.value ?: return@AdHandler
                val currentGold = user.stats?.gp ?: return@AdHandler
                compositeSubscription.add(userRepository.updateUser("stats.gp", currentGold + 100)
                    .flatMap { inventoryRepository.buyItem(user, "armoire", 100.0, 1) }
                    .subscribe({
                               configure(it.armoire["type"] ?: "",
                                   it.armoire["dropKey"] ?: "",
                                   it.armoire["dropText"] ?: "")
                        binding.adButton.updateForAdType(AdType.ARMOIRE, lifecycleScope)
                        hasAnimatedChanges = false
                        gold = null
                    }, RxErrorHandler.handleEmptyError()))
            }
            handler.prepare()
            binding.adButton.updateForAdType(AdType.ARMOIRE, lifecycleScope)
            binding.adButton.setOnClickListener {
                handler.show()
            }
        } else {
            binding.adButton.visibility = View.GONE
        }

        binding.closeButton.setOnClickListener {
            finish()
        }
        binding.equipButton.setOnClickListener {
            equipmentKey?.let { it1 -> inventoryRepository.equip("gear", it1).subscribe() }
            finish()
        }
        binding.dropRateButton.setOnClickListener {
            showDropRateDialog()
        }
        intent.extras?.let {
            val args = ArmoireActivityArgs.fromBundle(it)
            equipmentKey = args.key
            configure(args.type, args.key, args.text)
        }
    }

    override fun onResume() {
        super.onResume()
        startAnimation()
    }

    private fun startAnimation() {
        val gold = gold?.toInt()
        if (hasAnimatedChanges) return
        if (gold != null) {
            binding.goldView.value = (gold).toDouble()
            binding.goldView.value = (gold - 100).toDouble()
        }

        val container = binding.confettiAnchor
        container.postDelayed(
            {
                createParticles(container, R.drawable.confetti_blue)
                createParticles(container, R.drawable.confetti_red)
                createParticles(container, R.drawable.confetti_yellow)
                createParticles(container, R.drawable.confetti_purple)
            },
            500
        )
        hasAnimatedChanges = true
    }

    private fun createParticles(container: FrameLayout, resource: Int) {
        ParticleSystem(
            container,
            30,
            ContextCompat.getDrawable(this, resource),
            6000
        )
            .setRotationSpeed(144f)
            .setScaleRange(1.0f, 1.6f)
            .setSpeedByComponentsRange(-0.15f, 0.15f, 0.15f, 0.45f)
            .setFadeOut(200, AccelerateInterpolator())
            .emitWithGravity(binding.confettiAnchor, Gravity.TOP, 15, 2000)
    }

    fun configure(type: String, key: String, text: String) {
        binding.titleView.text = text
        binding.equipButton.visibility = if (type == "gear") View.VISIBLE else View.GONE
        when (type) {
            "gear" -> {
                binding.subtitleView.text = getString(R.string.armoireEquipment_new)
                binding.iconView.loadImage("shop_$key")
            }
            "food" -> {
                binding.subtitleView.text = getString(R.string.armoireFood_new)
                binding.iconView.loadImage("Pet_Food_$key")
            }
            else -> {
                binding.subtitleView.text = getString(R.string.armoireExp)
                binding.iconView.setImageResource(R.drawable.armoire_experience)
            }
        }
    }

    fun showDropRateDialog() {
        val dialog = HabiticaBottomSheetDialog(this)
        dialog.setContentView(R.layout.armoire_drop_rate_dialog)
        dialog.show()
    }
}