package com.mohanad.tv.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.mohanad.tv.R
import com.mohanad.tv.player.PlayerFactory

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_CHANNEL_URL = "extra_channel_url"
    }

    private lateinit var playerView: PlayerView
    private lateinit var nowPlayingText: TextView
    private lateinit var statusText: TextView
    private lateinit var dataSaverButton: TextView
    private var player: ExoPlayer? = null
    private var channelUrl: String = ""
    private var dataSaverEnabled: Boolean = false

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> statusText.text = getString(R.string.status_buffering)
                Player.STATE_READY -> statusText.text =
                    if (dataSaverEnabled) getString(R.string.status_low_quality) else ""
                Player.STATE_ENDED -> statusText.text = ""
                Player.STATE_IDLE -> {}
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            statusText.text = getString(R.string.error_load_failed)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.player_view)
        nowPlayingText = findViewById(R.id.text_now_playing)
        statusText = findViewById(R.id.text_player_status)
        dataSaverButton = findViewById(R.id.btn_data_saver)

        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME).orEmpty()
        channelUrl = intent.getStringExtra(EXTRA_CHANNEL_URL).orEmpty()
        nowPlayingText.text = channelName

        dataSaverButton.setOnClickListener { toggleDataSaver() }

        initializePlayer(channelUrl)
    }

    /**
     * يبدّل وضع توفير البيانات: يعيد بناء المشغل بنفس موضع التشغيل الحالي
     * لكن بمحدد مسارات يفرض أدنى جودة.
     */
    private fun toggleDataSaver() {
        dataSaverEnabled = !dataSaverEnabled
        dataSaverButton.isSelected = dataSaverEnabled
        val resumePosition = player?.currentPosition ?: 0L
        releasePlayer()
        initializePlayer(channelUrl, resumePosition)
    }

    private fun initializePlayer(url: String, resumePositionMs: Long = 0L) {
        if (url.isBlank()) return

        val exoPlayer = PlayerFactory.create(this, dataSaverMode = dataSaverEnabled)
        val mediaSource = PlayerFactory.buildMediaSource(this, url)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.addListener(playerListener)
        if (resumePositionMs > 0L) {
            exoPlayer.seekTo(resumePositionMs)
        }
        exoPlayer.playWhenReady = true
        exoPlayer.prepare()

        playerView.player = exoPlayer
        player = exoPlayer
    }

    private fun releasePlayer() {
        player?.let {
            it.removeListener(playerListener)
            it.release()
        }
        player = null
    }

    override fun onPause() {
        super.onPause()
        // نوقف التشغيل عند مغادرة الشاشة لتوفير البيانات والبطارية
        player?.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        player?.playWhenReady = true
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
    }
}
