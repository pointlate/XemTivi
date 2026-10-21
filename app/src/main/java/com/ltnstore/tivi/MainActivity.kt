package com.ltnstore.titvi

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ltnstore.titvi.data.ChannelRepository
import com.ltnstore.titvi.model.Channel
import com.ltnstore.titvi.ui.ChannelAdapter
import com.ltnstore.titvi.util.NumberBuffer
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var tvStatus: TextView
    private lateinit var tvNumberOverlay: TextView
    private lateinit var leftPanel: View
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: ChannelAdapter

    private lateinit var player: ExoPlayer
    private val repository = ChannelRepository()

    private var channels: List<Channel> = emptyList()
    private var currentChannel: Channel? = null

    private val numberBuffer = NumberBuffer(timeoutMs = 1000L) { number ->
        tvNumberOverlay.visibility = View.GONE
        jumpToNumber(number)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.player_view)
        tvStatus = findViewById(R.id.tv_status)
        tvNumberOverlay = findViewById(R.id.tv_number_overlay)
        leftPanel = findViewById(R.id.left_panel)
        recycler = findViewById(R.id.recycler_channels)

        adapter = ChannelAdapter(onClick = { channel -> playChannel(channel); hideLeftPanel() })
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        initPlayer()
        loadChannels()
    }

    private fun initPlayer() {
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this))
            .build()
        playerView.player = player
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                tvStatus.text = "Lỗi phát: ${error.errorCodeName}"
                tvStatus.visibility = View.VISIBLE
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) tvStatus.visibility = View.GONE
            }
        })
    }

    private fun loadChannels() {
        tvStatus.text = getString(R.string.loading)
        tvStatus.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val list = repository.loadChannels()
                channels = list
                adapter.submit(list)
                if (list.isEmpty()) {
                    tvStatus.text = getString(R.string.no_channels)
                } else {
                    // Tự phát kênh đầu tiên
                    playChannel(list.first())
                }
            } catch (e: Exception) {
                tvStatus.text = getString(R.string.error_network) + "\n(${e.message})"
            }
        }
    }

    private fun playChannel(channel: Channel) {
        currentChannel = channel
        adapter.setCurrent(channel.number)
        tvStatus.visibility = View.GONE
        val item = MediaItem.fromUri(channel.url)
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    private fun jumpToNumber(number: Int) {
        val ch = channels.firstOrNull { it.number == number }
        if (ch != null) {
            playChannel(ch)
        } else {
            tvStatus.text = "Không có kênh số $number"
            tvStatus.visibility = View.VISIBLE
            tvStatus.postDelayed({ tvStatus.visibility = View.GONE }, 1500)
        }
    }

    // ====== DANH SÁCH BÊN TRÁI ======
    private fun showLeftPanel() {
        leftPanel.visibility = View.VISIBLE
        recycler.requestFocus()
        val idx = channels.indexOfFirst { it.number == currentChannel?.number }
        if (idx >= 0) recycler.scrollToPosition(idx)
    }

    private fun hideLeftPanel() {
        leftPanel.visibility = View.GONE
        playerView.requestFocus()
    }

    private fun toggleLeftPanel() {
        if (leftPanel.visibility == View.VISIBLE) hideLeftPanel() else showLeftPanel()
    }

    // ====== XỬ LÝ PHÍM REMOTE ======
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            // Phím số 0..9 → nhập số kênh
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2,
            KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5,
            KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_8,
            KeyEvent.KEYCODE_9 -> {
                val digit = keyCode - KeyEvent.KEYCODE_0
                numberBuffer.onDigit(digit)
                tvNumberOverlay.text = numberBuffer.currentInput()
                tvNumberOverlay.visibility = View.VISIBLE
                return true
            }

            // MENU hoặc DPAD_LEFT → mở/đóng danh sách
            KeyEvent.KEYCODE_MENU -> {
                toggleLeftPanel()
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (leftPanel.visibility != View.VISIBLE) {
                    showLeftPanel()
                    return true
                }
            }

            // DPAD_RIGHT hoặc BACK → đóng danh sách
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (leftPanel.visibility == View.VISIBLE && recycler.hasFocus()) {
                    hideLeftPanel()
                    return true
                }
            }

            KeyEvent.KEYCODE_BACK -> {
                if (leftPanel.visibility == View.VISIBLE) {
                    hideLeftPanel()
                    return true
                }
            }

            // CH+/CH- chuyển kênh tuần tự
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                changeChannelByStep(+1)
                return true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                changeChannelByStep(-1)
                return true
            }

            // INFO/DPAD_CENTER → toggle danh sách nếu đang không focus
            KeyEvent.KEYCODE_INFO -> {
                toggleLeftPanel()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun changeChannelByStep(step: Int) {
        if (channels.isEmpty()) return
        val curr = currentChannel ?: channels.first()
        val idx = channels.indexOfFirst { it.number == curr.number }
        val next = ((idx + step).mod(channels.size))
        playChannel(channels[next])
    }

    override fun onDestroy() {
        numberBuffer.cancel()
        player.release()
        super.onDestroy()
    }
}