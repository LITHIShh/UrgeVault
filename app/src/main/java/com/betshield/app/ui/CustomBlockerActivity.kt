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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.betshield.app.BetShieldApplication
import com.betshield.app.databinding.ActivityCustomBlockerBinding
import com.betshield.app.databinding.ItemBlocklistEntryBinding
import com.betshield.app.databinding.ItemInstalledAppBinding
import com.betshield.app.monetization.AdManager
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomBlockerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomBlockerBinding
    private val app = BetShieldApplication.instance

    private val installedAppsList = mutableListOf<AppEntry>()
    private val filteredAppsList = mutableListOf<AppEntry>()
    private val customWebsitesList = mutableListOf<String>()
    private val filteredWebsitesList = mutableListOf<String>()

    private lateinit var appsAdapter: AppsAdapter
    private lateinit var websitesAdapter: WebsitesAdapter

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

        setupAdapters()
        setupListeners()
        loadInstalledApps()
        loadCustomWebsites()

        // Monetization: Load AdMob Banner & Preload Interstitial
        AdManager.loadBanner(binding.adBannerContainer, this)
        AdManager.loadInterstitial(this)
    }

    private fun setupAdapters() {
        appsAdapter = AppsAdapter(filteredAppsList)
        websitesAdapter = WebsitesAdapter(filteredWebsitesList)

        binding.rvCustomItems.layoutManager = LinearLayoutManager(this)
        binding.rvCustomItems.adapter = appsAdapter
    }

    private fun setupListeners() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if (tab?.position == 0) {
                    binding.rvCustomItems.adapter = appsAdapter
                    binding.btnAddDomain.visibility = View.GONE
                    binding.tilSearchInput.hint = "Search installed apps..."
                    filterApps(binding.etSearchOrAdd.text?.toString().orEmpty())
                } else {
                    binding.rvCustomItems.adapter = websitesAdapter
                    binding.btnAddDomain.visibility = View.VISIBLE
                    binding.tilSearchInput.hint = "Enter website (e.g. tiktok.com)..."
                    filterWebsites(binding.etSearchOrAdd.text?.toString().orEmpty())
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.etSearchOrAdd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                if (binding.tabLayout.selectedTabPosition == 0) {
                    filterApps(query)
                } else {
                    filterWebsites(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Add custom domain button
        binding.btnAddDomain.setOnClickListener {
            val domain = binding.etSearchOrAdd.text?.toString()?.trim()?.lowercase().orEmpty()
            if (domain.isNotEmpty() && domain.contains(".")) {
                app.preferencesManager.addCustomDomain(domain)
                binding.etSearchOrAdd.setText("")
                loadCustomWebsites()
                Toast.makeText(this, "Website added to blocklist: $domain", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a valid domain (e.g. instagram.com)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadInstalledApps() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
            val currentPkg = packageName
            val customBlocked = app.preferencesManager.customApps
            val defaultBlocked = app.blocklistDatabase.defaultAppPackages

            val apps = mutableListOf<AppEntry>()
            for (pkg in packages) {
                // Exclude system framework and BetShield itself
                val isSystem = (pkg.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (pkg.packageName == currentPkg || (isSystem && !pkg.packageName.contains("chrome") && !pkg.packageName.contains("browser"))) {
                    continue
                }

                val label = pkg.applicationInfo.loadLabel(pm).toString()
                val icon = pkg.applicationInfo.loadIcon(pm)
                val isBlocked = customBlocked.contains(pkg.packageName) || defaultBlocked.contains(pkg.packageName)

                apps.add(AppEntry(label, pkg.packageName, icon, isBlocked))
            }

            apps.sortBy { it.appName.lowercase() }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                installedAppsList.clear()
                installedAppsList.addAll(apps)
                filterApps(binding.etSearchOrAdd.text?.toString().orEmpty())
            }
        }
    }

    private fun loadCustomWebsites() {
        customWebsitesList.clear()
        customWebsitesList.addAll(app.preferencesManager.customDomains)
        filterWebsites(binding.etSearchOrAdd.text?.toString().orEmpty())
    }

    private fun filterApps(query: String) {
        val q = query.trim().lowercase()
        filteredAppsList.clear()
        if (q.isEmpty()) {
            filteredAppsList.addAll(installedAppsList)
        } else {
            for (app in installedAppsList) {
                if (app.appName.lowercase().contains(q) || app.packageName.lowercase().contains(q)) {
                    filteredAppsList.add(app)
                }
            }
        }
        appsAdapter.notifyDataSetChanged()
    }

    private fun filterWebsites(query: String) {
        val q = query.trim().lowercase()
        filteredWebsitesList.clear()
        if (q.isEmpty()) {
            filteredWebsitesList.addAll(customWebsitesList)
        } else {
            for (site in customWebsitesList) {
                if (site.lowercase().contains(q)) {
                    filteredWebsitesList.add(site)
                }
            }
        }
        websitesAdapter.notifyDataSetChanged()
    }

    // Apps Adapter
    inner class AppsAdapter(private val items: List<AppEntry>) :
        RecyclerView.Adapter<AppsAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemInstalledAppBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemInstalledAppBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvAppName.text = item.appName
            holder.binding.tvAppPackage.text = item.packageName
            if (item.icon != null) {
                holder.binding.ivAppIcon.setImageDrawable(item.icon)
            }

            holder.binding.switchBlockApp.setOnCheckedChangeListener(null)
            holder.binding.switchBlockApp.isChecked = item.isBlocked

            holder.binding.switchBlockApp.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    // Blocking app
                    item.isBlocked = true
                    app.preferencesManager.addCustomApp(item.packageName)
                    Toast.makeText(this@CustomBlockerActivity, "${item.appName} is now blocked", Toast.LENGTH_SHORT).show()
                } else {
                    // Unblocking app -> Require Password!
                    if (app.passwordManager.isPasswordSet()) {
                        PasswordDialog(
                            context = this@CustomBlockerActivity,
                            onSuccess = {
                                item.isBlocked = false
                                app.preferencesManager.removeCustomApp(item.packageName)
                                Toast.makeText(this@CustomBlockerActivity, "${item.appName} unblocked", Toast.LENGTH_SHORT).show()
                            },
                            onCancel = {
                                holder.binding.switchBlockApp.isChecked = true
                            }
                        ).show()
                    } else {
                        item.isBlocked = false
                        app.preferencesManager.removeCustomApp(item.packageName)
                    }
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    // Websites Adapter
    inner class WebsitesAdapter(private val items: List<String>) :
        RecyclerView.Adapter<WebsitesAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemBlocklistEntryBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemBlocklistEntryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val domain = items[position]
            holder.binding.tvItemName.text = domain
            holder.binding.tvItemCategory.text = "Custom Blocked Domain"
            holder.binding.tvItemStatus.text = "UNBLOCK"

            holder.binding.tvItemStatus.setOnClickListener {
                // Unblocking domain -> Require Password
                if (app.passwordManager.isPasswordSet()) {
                    PasswordDialog(
                        context = this@CustomBlockerActivity,
                        onSuccess = {
                            app.preferencesManager.removeCustomDomain(domain)
                            loadCustomWebsites()
                            Toast.makeText(this@CustomBlockerActivity, "$domain removed from blocklist", Toast.LENGTH_SHORT).show()
                        }
                    ).show()
                } else {
                    app.preferencesManager.removeCustomDomain(domain)
                    loadCustomWebsites()
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }

    override fun onDestroy() {
        // Keep user experience clean and uncluttered (no intrusive popups)
        super.onDestroy()
    }
}
