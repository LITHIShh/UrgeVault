package com.betshield.app.ui

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.ActivityCustomBlockerBinding
import com.betshield.app.databinding.ItemBlocklistEntryBinding
import com.betshield.app.databinding.ItemInstalledAppBinding
import com.betshield.app.monetization.AdManager
import com.betshield.app.util.HapticEngine
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomBlockerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomBlockerBinding
    private val app = BetShieldApplication.instance
    private val prefs by lazy { app.preferencesManager }
    private val blocklist by lazy { app.blocklistDatabase }
    private val passwordManager by lazy { app.passwordManager }

    private val installedAppsList = mutableListOf<AppEntry>()
    private val filteredAppsList = mutableListOf<AppEntry>()
    private val customWebsitesList = mutableListOf<String>()

    private val preloadedBettingList = mutableListOf<Pair<String, String>>() // Name, Type
    private val filteredBettingList = mutableListOf<Pair<String, String>>()

    private lateinit var appsAdapter: AppsAdapter
    private lateinit var websitesAdapter: CustomWebsitesAdapter
    private lateinit var preloadedAdapter: PreloadedBettingAdapter

    data class AppEntry(
        val appName: String,
        val packageName: String,
        val icon: Drawable?,
        var isBlocked: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomBlockerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupCategories()
        setupCustomWebsites()
        setupInstalledApps()
        setupPreloadedBetting()
        updateBadgeCount()

        // Load AdMob banner at bottom
        AdManager.loadBanner(binding.adBannerContainer, this)
    }

    private fun setupTabs() {
        binding.tabLayoutBlocker.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                HapticEngine.performClick(binding.tabLayoutBlocker)
                binding.containerTabCategories.visibility = if (tab?.position == 0) View.VISIBLE else View.GONE
                binding.containerTabWebsites.visibility = if (tab?.position == 1) View.VISIBLE else View.GONE
                binding.containerTabApps.visibility = if (tab?.position == 2) View.VISIBLE else View.GONE
                binding.containerTabPreloadedBetting.visibility = if (tab?.position == 3) View.VISIBLE else View.GONE

                if (tab?.position == 2 && installedAppsList.isEmpty()) {
                    loadInstalledApps()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupCategories() {
        binding.switchBlockGambling.isChecked = prefs.isGamblingBlockingEnabled
        binding.switchBlockAdult.isChecked = prefs.isAdultBlockingEnabled
        binding.switchBlockSocial.isChecked = prefs.isSocialMediaBlockingEnabled
        binding.switchBlockGaming.isChecked = prefs.isGamingBlockingEnabled
        binding.switchBlockCrypto.isChecked = prefs.isCryptoBlockingEnabled

        binding.switchBlockGambling.setOnCheckedChangeListener { _, isChecked ->
            handleCategoryToggle(
                targetSwitch = binding.switchBlockGambling,
                isChecked = isChecked,
                onApproved = {
                    prefs.isGamblingBlockingEnabled = isChecked
                    updateBadgeCount()
                }
            )
        }

        binding.switchBlockAdult.setOnCheckedChangeListener { _, isChecked ->
            handleCategoryToggle(
                targetSwitch = binding.switchBlockAdult,
                isChecked = isChecked,
                onApproved = {
                    prefs.isAdultBlockingEnabled = isChecked
                    updateBadgeCount()
                }
            )
        }

        binding.switchBlockSocial.setOnCheckedChangeListener { _, isChecked ->
            prefs.isSocialMediaBlockingEnabled = isChecked
            updateBadgeCount()
        }

        binding.switchBlockGaming.setOnCheckedChangeListener { _, isChecked ->
            prefs.isGamingBlockingEnabled = isChecked
            updateBadgeCount()
        }

        binding.switchBlockCrypto.setOnCheckedChangeListener { _, isChecked ->
            prefs.isCryptoBlockingEnabled = isChecked
            updateBadgeCount()
        }
    }

    private fun handleCategoryToggle(targetSwitch: MaterialSwitch, isChecked: Boolean, onApproved: () -> Unit) {
        if (!isChecked && passwordManager.isPasswordSet()) {
            PasswordDialog(
                context = this,
                onSuccess = { onApproved() },
                onCancel = { targetSwitch.isChecked = true }
            ).show()
        } else {
            onApproved()
        }
    }

    private fun setupCustomWebsites() {
        websitesAdapter = CustomWebsitesAdapter(customWebsitesList) { domain ->
            // Delete custom domain with PIN check
            if (passwordManager.isPasswordSet()) {
                PasswordDialog(
                    context = this,
                    onSuccess = {
                        prefs.removeCustomDomain(domain)
                        loadCustomWebsites()
                        updateBadgeCount()
                        Toast.makeText(this, "Unblocked: $domain", Toast.LENGTH_SHORT).show()
                    }
                ).show()
            } else {
                prefs.removeCustomDomain(domain)
                loadCustomWebsites()
                updateBadgeCount()
                Toast.makeText(this, "Unblocked: $domain", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvCustomWebsites.layoutManager = LinearLayoutManager(this)
        binding.rvCustomWebsites.adapter = websitesAdapter

        binding.btnSubmitCustomDomain.setOnClickListener {
            val input = binding.etCustomDomainInput.text.toString().trim()
            if (input.isNotEmpty()) {
                prefs.addCustomDomain(input)
                binding.etCustomDomainInput.text?.clear()
                loadCustomWebsites()
                updateBadgeCount()
                Toast.makeText(this, "Added to Blocklist: $input", Toast.LENGTH_SHORT).show()
            }
        }

        loadCustomWebsites()
    }

    private fun loadCustomWebsites() {
        customWebsitesList.clear()
        customWebsitesList.addAll(prefs.customDomains.sorted())
        websitesAdapter.notifyDataSetChanged()
    }

    private fun setupInstalledApps() {
        appsAdapter = AppsAdapter(filteredAppsList) { entry, isBlocked ->
            if (!isBlocked && passwordManager.isPasswordSet()) {
                // Turning OFF an app block -> requires PIN
                PasswordDialog(
                    context = this,
                    onSuccess = {
                        prefs.removeCustomApp(entry.packageName)
                        entry.isBlocked = false
                        appsAdapter.notifyDataSetChanged()
                        updateBadgeCount()
                    },
                    onCancel = {
                        entry.isBlocked = true
                        appsAdapter.notifyDataSetChanged()
                    }
                ).show()
            } else {
                if (isBlocked) {
                    prefs.addCustomApp(entry.packageName)
                    entry.isBlocked = true
                } else {
                    prefs.removeCustomApp(entry.packageName)
                    entry.isBlocked = false
                }
                updateBadgeCount()
            }
        }

        binding.rvInstalledApps.layoutManager = LinearLayoutManager(this)
        binding.rvInstalledApps.adapter = appsAdapter

        binding.etSearchApps.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterInstalledApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadInstalledApps() {
        binding.progressLoadingApps.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val currentPkg = packageName
            val blockedApps = prefs.customApps

            val apps = packages
                .filter { it.packageName != currentPkg && (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { appInfo ->
                    AppEntry(
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        packageName = appInfo.packageName,
                        icon = pm.getApplicationIcon(appInfo),
                        isBlocked = blockedApps.contains(appInfo.packageName)
                    )
                }
                .sortedBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                installedAppsList.clear()
                installedAppsList.addAll(apps)
                filterInstalledApps(binding.etSearchApps.text.toString())
                binding.progressLoadingApps.visibility = View.GONE
            }
        }
    }

    private fun filterInstalledApps(query: String) {
        filteredAppsList.clear()
        if (query.isBlank()) {
            filteredAppsList.addAll(installedAppsList)
        } else {
            val lower = query.lowercase()
            filteredAppsList.addAll(installedAppsList.filter {
                it.appName.lowercase().contains(lower) || it.packageName.lowercase().contains(lower)
            })
        }
        appsAdapter.notifyDataSetChanged()
    }

    private fun setupPreloadedBetting() {
        preloadedBettingList.clear()
        blocklist.gamblingDomains.sorted().forEach { preloadedBettingList.add(Pair(it, "Betting Website")) }
        blocklist.gamblingAppPackages.sorted().forEach { preloadedBettingList.add(Pair(it, "Betting App Package")) }
        filteredBettingList.addAll(preloadedBettingList)

        preloadedAdapter = PreloadedBettingAdapter(filteredBettingList)
        binding.rvPreloadedBetting.layoutManager = LinearLayoutManager(this)
        binding.rvPreloadedBetting.adapter = preloadedAdapter

        binding.etSearchPreloadedBetting.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString().orEmpty().lowercase()
                filteredBettingList.clear()
                if (q.isBlank()) {
                    filteredBettingList.addAll(preloadedBettingList)
                } else {
                    filteredBettingList.addAll(preloadedBettingList.filter {
                        it.first.lowercase().contains(q)
                    })
                }
                preloadedAdapter.notifyDataSetChanged()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateBadgeCount() {
        val total = blocklist.getTotalBlockedCount()
        binding.tvTotalBlockedBadge.text = "$total Active Items Blocked"
    }

    // ==========================================
    // ADAPTERS
    // ==========================================

    inner class CustomWebsitesAdapter(
        private val items: List<String>,
        private val onDeleteClick: (String) -> Unit
    ) : RecyclerView.Adapter<CustomWebsitesAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemBlocklistEntryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemBlocklistEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val domain = items[position]
            holder.binding.tvItemName.text = domain
            holder.binding.tvItemCategory.text = "Custom User Blocked Website"
            holder.binding.tvItemStatus.text = "DELETE"
            holder.binding.tvItemStatus.setTextColor(ContextCompat.getColor(this@CustomBlockerActivity, R.color.danger))
            holder.binding.tvItemStatus.setOnClickListener { onDeleteClick(domain) }
            holder.binding.ivItemType.setImageResource(R.drawable.ic_block)
        }

        override fun getItemCount(): Int = items.size
    }

    inner class AppsAdapter(
        private val items: List<AppEntry>,
        private val onToggle: (AppEntry, Boolean) -> Unit
    ) : RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemInstalledAppBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemInstalledAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvAppName.text = item.appName
            holder.binding.tvAppPackage.text = item.packageName
            holder.binding.ivAppIcon.setImageDrawable(item.icon)

            holder.binding.switchBlockApp.setOnCheckedChangeListener(null)
            holder.binding.switchBlockApp.isChecked = item.isBlocked
            holder.binding.switchBlockApp.setOnCheckedChangeListener { _, isChecked ->
                onToggle(item, isChecked)
            }
        }

        override fun getItemCount(): Int = items.size
    }

    inner class PreloadedBettingAdapter(
        private val items: List<Pair<String, String>>
    ) : RecyclerView.Adapter<PreloadedBettingAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemBlocklistEntryBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val b = ItemBlocklistEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(b)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvItemName.text = item.first
            holder.binding.tvItemCategory.text = item.second
            holder.binding.tvItemStatus.text = "PROTECTED"
            holder.binding.tvItemStatus.setTextColor(ContextCompat.getColor(this@CustomBlockerActivity, R.color.primary))
        }

        override fun getItemCount(): Int = items.size
    }
}
