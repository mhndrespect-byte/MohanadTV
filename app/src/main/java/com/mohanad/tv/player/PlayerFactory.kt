package com.mohanad.tv.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.common.MediaItem

/**
 * مصنع مسؤول عن بناء ExoPlayer مهيّأ بالكامل للتعامل مع الشبكات الضعيفة.
 *
 * الفلسفة هنا:
 * 1) زيادة زمن الـ Buffer قبل بدء التشغيل (بدل التقطيع، ننتظر أكثر مرة واحدة).
 * 2) تفعيل AdaptiveTrackSelection بحيث يختار المشغّل تلقائياً أدنى جودة
 *    متاحة عندما يكتشف ضعف عرض النطاق (bandwidth)، ويرفعها تدريجياً
 *    فقط عند تأكد استقرار الشبكة (لتفادي التذبذب/القفزات المفرطة في الجودة).
 * 3) تقليل استهلاك البيانات عبر تفضيل الجودة الأدنى عند بدء التشغيل أولاً.
 */
@UnstableApi
object PlayerFactory {

    // ===== ضبط الـ Buffer المخصص للشبكات الضعيفة =====
    // أقل مدة بالميلي ثانية للتشغيل المتواصل بدون توقف لإعادة التخزين
    private const val MIN_BUFFER_MS = 30_000        // 30 ثانية (الافتراضي في ExoPlayer هو 15)
    // أعلى مدة يمكن تخزينها مسبقاً (نحفظ بيانات أكثر لتفادي القطع المتكرر)
    private const val MAX_BUFFER_MS = 60_000        // دقيقة كاملة
    // المدة المطلوبة قبل بدء التشغيل لأول مرة (Buffering الأولي)
    private const val BUFFER_FOR_PLAYBACK_MS = 4_000
    // المدة المطلوبة بعد توقف بسبب نفاذ الـ Buffer قبل إعادة التشغيل
    private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 6_000

    // ===== ضبط اختيار الجودة التلقائي (Adaptive Bitrate) =====
    // أقصى وقت انتظار لتحديد عرض النطاق قبل اتخاذ قرار الجودة (نزيده لتفادي قرارات خاطئة بسرعة)
    private const val MIN_DURATION_FOR_QUALITY_INCREASE_MS = 12_000
    private const val MAX_DURATION_FOR_QUALITY_DECREASE_MS = 30_000
    private const val MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 30_000
    // هامش أمان من عرض النطاق الحقيقي (نحجز 30% احتياط لتفادي التقطيع المفاجئ)
    private const val BANDWIDTH_FRACTION = 0.7f

    /**
     * يبني نسخة ExoPlayer جاهزة بكل الضبط أعلاه.
     * @param dataSaverMode إن كان true، يُجبر المشغل على أقل جودة دائماً
     *        (مفيد عند شبكة بيانات جوال محدودة جداً).
     */
    fun create(context: Context, dataSaverMode: Boolean = false): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            // نسمح بالتشغيل حتى لو لم يصل الـ Buffer لأقصاه، فقط نحتاج الحد الأدنى
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // مقياس عرض النطاق - يراقب سرعة التنزيل الفعلية باستمرار
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context)
            .build()

        // محدد المسارات التكيفي - هو من يقرر الجودة تلقائياً حسب الشبكة
        val trackSelectionFactory = AdaptiveTrackSelection.Factory(
            MIN_DURATION_FOR_QUALITY_INCREASE_MS,
            MAX_DURATION_FOR_QUALITY_DECREASE_MS,
            MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
            BANDWIDTH_FRACTION
        )
        val trackSelector = DefaultTrackSelector(context, trackSelectionFactory).apply {
            setParameters(
                buildUponParameters()
                    // true = إجبار أقل جودة دائماً (وضع توفير البيانات الصريح)
                    // false = نسمح للمشغل بالتكيف الذكي حسب الشبكة بدل تجميد الجودة على الأدنى دائماً
                    .setForceLowestBitrate(dataSaverMode)
                    // نسمح للمشغل بتجاوز قيود الدقة العالية تلقائياً عند ضعف الشبكة
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setTunnelingEnabled(false)
            )
        }

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setBandwidthMeter(bandwidthMeter)
            .build()
    }

    /**
     * يبني MediaSource مناسب لرابط القناة (HLS أو رابط مباشر).
     * يُستخدم DataSource.Factory مع مهلات زمنية مرنة تتحمل بطء الشبكة.
     */
    fun buildMediaSource(context: Context, url: String): MediaSource {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("MohanadTV/1.0")

        val mediaItem = MediaItem.fromUri(url)

        return if (url.contains(".m3u8") || url.contains("/live/") || url.contains("get.php")) {
            HlsMediaSource.Factory(httpDataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            androidx.media3.exoplayer.source.ProgressiveMediaSource.Factory(httpDataSourceFactory)
                .createMediaSource(mediaItem)
        }
    }
}
