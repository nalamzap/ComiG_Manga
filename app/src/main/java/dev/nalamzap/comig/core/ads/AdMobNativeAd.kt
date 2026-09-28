package dev.nalamzap.comig.core.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

private const val TEST_NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110"

@Composable
fun AdMobNativeAd(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_NATIVE_AD_UNIT_ID
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadFailed by remember { mutableStateOf(false) }

    DisposableEffect(adUnitId) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd?.destroy()
                nativeAd = ad
                isLoading = false
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    loadFailed = true
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (loadFailed) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (nativeAd != null) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                factory = { ctx ->
                    val adView = NativeAdView(ctx)

                    val rootLayout = LinearLayout(ctx).apply {
                        orientation = LinearLayout.VERTICAL
                    }

                    // Header Row: Badge + Icon + Headline
                    val headerRow = LinearLayout(ctx).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }

                    // Ad Badge
                    val badgeBackground = GradientDrawable().apply {
                        setColor(Color.parseColor("#7C4DFF"))
                        cornerRadius = 8f
                    }
                    val badge = TextView(ctx).apply {
                        text = " AD "
                        textSize = 10f
                        background = badgeBackground
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        setPadding(12, 4, 12, 4)
                    }

                    // Icon View
                    val iconView = ImageView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(90, 90).apply {
                            setMargins(16, 0, 16, 0)
                        }
                    }
                    adView.iconView = iconView

                    // Headline
                    val headline = TextView(ctx).apply {
                        textSize = 15f
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        maxLines = 1
                    }
                    adView.headlineView = headline

                    headerRow.addView(badge)
                    headerRow.addView(iconView)
                    headerRow.addView(headline, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                    rootLayout.addView(headerRow)

                    // Media View (Image/Video)
                    val mediaView = MediaView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            360
                        ).apply {
                            topMargin = 16
                            bottomMargin = 12
                        }
                    }
                    adView.mediaView = mediaView
                    rootLayout.addView(mediaView)

                    // Body Text
                    val body = TextView(ctx).apply {
                        textSize = 13f
                        setTextColor(Color.parseColor("#CCCCCC"))
                        setPadding(0, 0, 0, 12)
                        maxLines = 2
                    }
                    adView.bodyView = body
                    rootLayout.addView(body)

                    // CTA Button with custom styled gradient/background
                    val ctaBg = GradientDrawable().apply {
                        setColor(Color.parseColor("#6200EE"))
                        cornerRadius = 24f
                    }
                    val cta = Button(ctx).apply {
                        textSize = 14f
                        background = ctaBg
                        setTextColor(Color.WHITE)
                        setTypeface(null, Typeface.BOLD)
                        setPadding(24, 8, 24, 8)
                    }
                    adView.callToActionView = cta
                    rootLayout.addView(cta, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ))

                    adView.addView(rootLayout)
                    adView
                },
                update = { adView ->
                    val ad = nativeAd ?: return@AndroidView
                    (adView.headlineView as? TextView)?.text = ad.headline
                    (adView.bodyView as? TextView)?.let {
                        it.text = ad.body ?: ""
                        it.visibility = if (ad.body != null) View.VISIBLE else View.GONE
                    }
                    (adView.iconView as? ImageView)?.let {
                        if (ad.icon != null) {
                            it.setImageDrawable(ad.icon?.drawable)
                            it.visibility = View.VISIBLE
                        } else {
                            it.visibility = View.GONE
                        }
                    }
                    (adView.callToActionView as? Button)?.let {
                        it.text = ad.callToAction ?: "Learn More"
                        it.visibility = if (ad.callToAction != null) View.VISIBLE else View.GONE
                    }
                    adView.setNativeAd(ad)
                }
            )
        }
    }
}
