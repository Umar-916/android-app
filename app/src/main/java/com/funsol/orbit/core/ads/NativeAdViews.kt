package com.funsol.orbit.core.ads

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.funsol.orbit.R
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesView
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import java.util.Locale

internal class NativeAdPalette(
    val accent: Int,
    val text: Int,
    val muted: Int,
    val ctaFill: Int,
    val ctaText: Int,
)

internal object NativeAdMetrics {
    const val ICON_DP = 40
    const val CTA_DP = 40
    const val CTA_RADIUS_DP = 10
    const val HEADER_DP = 40
    const val COMPACT_GAP_DP = 10
    const val LARGE_MEDIA_DP = 110
    const val LARGE_GAP_DP = 8

    const val COMPACT_CONTENT_DP = HEADER_DP + COMPACT_GAP_DP + CTA_DP

    const val LARGE_CONTENT_DP = HEADER_DP + LARGE_GAP_DP + LARGE_MEDIA_DP + LARGE_GAP_DP + 16 + LARGE_GAP_DP + CTA_DP

    const val COMPACT_MAX_DP = COMPACT_CONTENT_DP + 14
    const val LARGE_MAX_DP = LARGE_CONTENT_DP + 18
}

internal fun buildNativeAdView(context: Context, ad: NativeAd, large: Boolean, p: NativeAdPalette): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun dp(v: Int) = (v * density).toInt()

    fun text(value: String, sizeSp: Float, color: Int, bold: Boolean = false, maxLines: Int = 1) = TextView(context).apply {
        this.text = value
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        this.maxLines = maxLines
        ellipsize = TextUtils.TruncateAt.END
        includeFontPadding = false
    }

    val adView = NativeAdView(context)
    val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    val header = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, 0, dp(18), 0)
    }
    val iconDrawable = ad.icon?.drawable
    if (iconDrawable != null) {
        val icon = ImageView(context).apply {
            setImageDrawable(iconDrawable)
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = roundedOutline(dp(8))
        }
        header.addView(icon, LinearLayout.LayoutParams(dp(NativeAdMetrics.ICON_DP), dp(NativeAdMetrics.ICON_DP)).apply { marginEnd = dp(10) })
        adView.iconView = icon
    }
    val titles = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val titleRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    ad.headline?.let { headline ->
        val title = text(headline, 15f, p.text, bold = true)
        titleRow.addView(title, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(6) })
        adView.headlineView = title
    }
    val attribution = text(context.getString(R.string.ads_label).uppercase(Locale.getDefault()), 10f, p.accent, bold = true).apply {
        background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
            setStroke(dp(1), p.accent)
        }
        setPadding(dp(4), dp(1), dp(4), dp(1))
    }
    titleRow.addView(attribution, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    titles.addView(titleRow, LinearLayout.LayoutParams(-1, -2))

    var bodyView: TextView? = null
    ad.body?.takeIf { it.isNotBlank() }?.let { value ->
        bodyView = text(value, 12f, p.muted, maxLines = if (large) 2 else 1)
    }
    if (!large) {
        bodyView?.let { body ->
            titles.addView(body, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
            adView.bodyView = body
        }
    } else {
        val facts = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        ad.advertiser?.takeIf { it.isNotBlank() }?.let { name ->
            val advertiser = text(name, 11f, p.muted)
            facts.addView(advertiser, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
            adView.advertiserView = advertiser
        }
        ad.starRating?.let { rating ->
            val stars = text(String.format(Locale.getDefault(), "★ %.1f", rating), 11f, p.text, bold = true)
            facts.addView(stars, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
            adView.starRatingView = stars
        }
        ad.price?.takeIf { it.isNotBlank() }?.let { value ->
            val price = text(value, 11f, p.muted)
            facts.addView(price, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
            adView.priceView = price
        }
        ad.store?.takeIf { it.isNotBlank() }?.let { value ->
            val store = text(value, 11f, p.muted)
            facts.addView(store, LinearLayout.LayoutParams(-2, -2))
            adView.storeView = store
        }
        if (facts.childCount > 0) titles.addView(facts, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(3) })
    }
    header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    column.addView(header, LinearLayout.LayoutParams(-1, dp(NativeAdMetrics.HEADER_DP)))

    var mediaView: MediaView? = null
    if (large) {
        val media = MediaView(context)
        val frame = FrameLayout(context).apply {
            clipToOutline = true
            outlineProvider = roundedOutline(dp(10))
        }
        frame.addView(media, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        column.addView(frame, LinearLayout.LayoutParams(-1, dp(NativeAdMetrics.LARGE_MEDIA_DP)).apply { topMargin = dp(NativeAdMetrics.LARGE_GAP_DP) })
        mediaView = media
        bodyView?.let { body ->
            column.addView(body, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(NativeAdMetrics.LARGE_GAP_DP) })
            adView.bodyView = body
        }
    }

    ad.callToAction?.takeIf { it.isNotBlank() }?.let { label ->
        val cta = Button(context).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(p.ctaText)
            typeface = Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            setPadding(dp(12), 0, dp(12), 0)
            stateListAnimator = null
            elevation = 0f
            background = GradientDrawable().apply {
                cornerRadius = dp(NativeAdMetrics.CTA_RADIUS_DP).toFloat()
                setColor(p.ctaFill)
            }
        }
        val gap = if (large) NativeAdMetrics.LARGE_GAP_DP else NativeAdMetrics.COMPACT_GAP_DP
        column.addView(cta, LinearLayout.LayoutParams(-1, dp(NativeAdMetrics.CTA_DP)).apply { topMargin = dp(gap) })
        adView.callToActionView = cta
    }

    adView.addView(column, FrameLayout.LayoutParams(-1, -2))

    val adChoices = AdChoicesView(context)
    adView.addView(adChoices, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.RIGHT))
    adView.adChoicesView = adChoices

    adView.registerNativeAd(ad, mediaView)
    return adView
}

private fun roundedOutline(radiusPx: Int) = object : ViewOutlineProvider() {
    override fun getOutline(view: View, outline: android.graphics.Outline) {
        outline.setRoundRect(0, 0, view.width, view.height, radiusPx.toFloat())
    }
}
