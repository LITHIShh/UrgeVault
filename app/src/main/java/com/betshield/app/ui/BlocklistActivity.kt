package com.betshield.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.betshield.app.BetShieldApplication
import com.betshield.app.R
import com.betshield.app.databinding.ActivityBlocklistBinding
import com.betshield.app.databinding.ItemBlocklistEntryBinding

class BlocklistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlocklistBinding
    private val app = BetShieldApplication.instance
    private val allEntries = mutableListOf<BlockItem>()
    private val filteredEntries = mutableListOf<BlockItem>()
    private lateinit var adapter: BlocklistAdapter

    data class BlockItem(
        val name: String,
        val type: ItemType,
        val isCustom: Boolean = false
    )

    enum class ItemType {
        WEBSITE, APP
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlocklistBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadBlocklistData()
        setupListeners()
    }

    private fun setupRecyclerView() {
        adapter = BlocklistAdapter(filteredEntries)
        binding.rvBlocklist.layoutManager = LinearLayoutManager(this)
        binding.rvBlocklist.adapter = adapter
    }

    private fun loadBlocklistData() {
        allEntries.clear()

        // Curated Domains (Gambling, Adult, Social, Gaming, Crypto)
        for (domain in app.blocklistDatabase.gamblingDomains) {
            allEntries.add(BlockItem(domain, ItemType.WEBSITE))
        }
        for (domain in app.blocklistDatabase.adultDomains) {
            allEntries.add(BlockItem(domain, ItemType.WEBSITE))
        }

        // Curated Apps
        for (appPkg in app.blocklistDatabase.gamblingAppPackages) {
            allEntries.add(BlockItem(appPkg, ItemType.APP))
        }
        for (appPkg in app.blocklistDatabase.socialAppPackages) {
            allEntries.add(BlockItem(appPkg, ItemType.APP))
        }

        // Custom Domains
        for (domain in app.preferencesManager.customDomains) {
            allEntries.add(BlockItem(domain, ItemType.WEBSITE, isCustom = true))
        }

        // Custom Apps
        for (appPkg in app.preferencesManager.customApps) {
            allEntries.add(BlockItem(appPkg, ItemType.APP, isCustom = true))
        }

        filterList()
    }

    private fun setupListeners() {
        binding.etSearchBlocklist.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, _ ->
            filterList()
        }

        binding.fabAddCustom.setOnClickListener {
            // Require password to add/modify rules
            if (app.passwordManager.isPasswordSet() && !app.passwordManager.isTemporarilyUnlocked()) {
                PasswordDialog(this, onSuccess = {
                    showAddCustomDialog()
                }).show()
            } else {
                showAddCustomDialog()
            }
        }
    }

    private fun filterList() {
        val query = binding.etSearchBlocklist.text?.toString()?.trim()?.lowercase() ?: ""
        val filterWebsites = binding.chipWebsites.isChecked
        val filterApps = binding.chipApps.isChecked
        val filterCustom = binding.chipCustom.isChecked

        filteredEntries.clear()
        for (item in allEntries) {
            val matchesQuery = query.isEmpty() || item.name.lowercase().contains(query)
            val matchesCategory = when {
                filterCustom -> item.isCustom
                filterWebsites && filterApps -> true
                filterWebsites -> item.type == ItemType.WEBSITE
                filterApps -> item.type == ItemType.APP
                else -> true
            }

            if (matchesQuery && matchesCategory) {
                filteredEntries.add(item)
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showAddCustomDialog() {
        val input = EditText(this).apply {
            hint = "e.g. gambling-site.com or app.package.name"
        }
        AlertDialog.Builder(this)
            .setTitle("Add Custom Block Rule")
            .setMessage("Enter the domain name or Android package name to block:")
            .setView(input)
            .setPositiveButton("Block") { _, _ ->
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    if (text.contains(".")) {
                        app.preferencesManager.addCustomDomain(text)
                        Toast.makeText(this, "Added to blocklist: $text", Toast.LENGTH_SHORT).show()
                        loadBlocklistData()
                    } else {
                        Toast.makeText(this, "Please enter a valid domain or package.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    inner class BlocklistAdapter(private val items: List<BlockItem>) :
        RecyclerView.Adapter<BlocklistAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemBlocklistEntryBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemBinding = ItemBlocklistEntryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvItemName.text = item.name
            holder.binding.tvItemCategory.text = if (item.type == ItemType.WEBSITE) {
                if (item.isCustom) "Custom Domain Rule" else "Gambling Website"
            } else {
                if (item.isCustom) "Custom App Rule" else "Betting Application"
            }
            holder.binding.tvItemStatus.text = "BLOCKED"
        }

        override fun getItemCount(): Int = items.size
    }
}
