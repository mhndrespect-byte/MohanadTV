package com.mohanad.tv.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mohanad.tv.R
import com.mohanad.tv.model.Channel
import com.mohanad.tv.model.PlaylistParser
import com.mohanad.tv.util.NetworkUtil
import com.mohanad.tv.util.PrefsManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var editUrl: EditText
    private lateinit var editSearch: EditText
    private lateinit var btnLoad: Button
    private lateinit var progressLoading: ProgressBar
    private lateinit var recyclerChannels: RecyclerView
    private lateinit var recyclerCategories: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var networkStatusText: TextView

    private lateinit var channelAdapter: ChannelAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    // Executor خفيف بدل أي مكتبة threading ثقيلة (Coroutines/RxJava غير مستخدمة عمداً للحفاظ على الخفة)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentCategory: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupAdapters()
        setupListeners()
        restoreLastUrl()
        updateNetworkIndicator()
    }

    private fun bindViews() {
        editUrl = findViewById(R.id.edit_m3u_url)
        editSearch = findViewById(R.id.edit_search)
        btnLoad = findViewById(R.id.btn_load_playlist)
        progressLoading = findViewById(R.id.progress_loading)
        recyclerChannels = findViewById(R.id.recycler_channels)
        recyclerCategories = findViewById(R.id.recycler_categories)
        emptyStateLayout = findViewById(R.id.layout_empty_state)
        networkStatusText = findViewById(R.id.text_network_status)
    }

    private fun setupAdapters() {
        channelAdapter = ChannelAdapter { channel -> openPlayer(channel) }
        recyclerChannels.layoutManager = LinearLayoutManager(this)
        recyclerChannels.adapter = channelAdapter
        recyclerChannels.setHasFixedSize(true)

        categoryAdapter = CategoryAdapter { category ->
            currentCategory = category
            channelAdapter.filter(editSearch.text.toString(), currentCategory)
        }
        recyclerCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerCategories.adapter = categoryAdapter
    }

    private fun setupListeners() {
        btnLoad.setOnClickListener { onLoadClicked() }

        editUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onLoadClicked()
                true
            } else {
                false
            }
        }

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                channelAdapter.filter(s?.toString().orEmpty(), currentCategory)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun restoreLastUrl() {
        val lastUrl = PrefsManager.getLastUrl(this)
        if (!lastUrl.isNullOrBlank()) {
            editUrl.setText(lastUrl)
        }
    }

    private fun updateNetworkIndicator() {
        val connected = NetworkUtil.isConnected(this)
        networkStatusText.setTextColor(
            if (connected) getColorCompat(R.color.green_terminal)
            else getColorCompat(R.color.red_error)
        )
    }

    private fun getColorCompat(resId: Int): Int {
        return ContextCompat.getColor(this, resId)
    }

    private fun onLoadClicked() {
        val url = editUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_invalid_url), Toast.LENGTH_SHORT).show()
            return
        }

        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, getString(R.string.error_load_failed), Toast.LENGTH_SHORT).show()
            return
        }

        setLoadingState(true)
        executor.execute {
            try {
                val channels = PlaylistParser.fetch(url)
                mainHandler.post {
                    setLoadingState(false)
                    if (channels.isEmpty()) {
                        Toast.makeText(this, getString(R.string.error_empty_playlist), Toast.LENGTH_SHORT).show()
                    } else {
                        PrefsManager.saveLastUrl(this, url)
                        showChannels(channels)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    setLoadingState(false)
                    Toast.makeText(this, getString(R.string.error_load_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setLoadingState(loading: Boolean) {
        progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        btnLoad.isEnabled = !loading
        btnLoad.text = if (loading) getString(R.string.btn_load_loading) else getString(R.string.btn_load)
    }

    private fun showChannels(channels: List<Channel>) {
        channelAdapter.submitList(channels)
        categoryAdapter.submitCategories(channelAdapter.getCategories())
        currentCategory = null

        emptyStateLayout.visibility = View.GONE
        recyclerChannels.visibility = View.VISIBLE
        recyclerCategories.visibility = View.VISIBLE
        editSearch.visibility = View.VISIBLE
    }

    private fun openPlayer(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_CHANNEL_NAME, channel.name)
        intent.putExtra(PlayerActivity.EXTRA_CHANNEL_URL, channel.url)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateNetworkIndicator()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
