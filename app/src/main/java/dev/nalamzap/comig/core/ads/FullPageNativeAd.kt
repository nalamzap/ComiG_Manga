package dev.nalamzap.comig.core.ads

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun FullPageNativeAd(
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor(0xFF141418)),
        contentAlignment = Alignment.Center
    ) {
        if (loadFailed) {
            // Soothing fallback placeholder if ad fails to load
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    "Break Time ☕",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ComposeColor.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Take a quick breather before your next page!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ComposeColor.LightGray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Swipe to continue", color = ComposeColor.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = ComposeColor.Gray)
                }
            }
        } else if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        } else if (nativeAd != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ComposeColor.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        "SPONSORED",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = ComposeColor.White,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                // Native Ad Content View
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    factory = { ctx ->
                        val adView = NativeAdView(ctx)

                        val rootLayout = LinearLayout(ctx).apply {
                            orientation = LinearLayout.VERTICAL
                            gravity = android.view.Gravity.CENTER
                        }

                        // Icon + Headline Row
                        val headerRow = LinearLayout(ctx).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER
                            setPadding(0, 0, 0, 12)
                        }

                        val iconView = ImageView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                                rightMargin = 16
                            }
                        }
                        adView.iconView = iconView

                        val headline = TextView(ctx).apply {
                            textSize = 18f
                            setTextColor(Color.WHITE)
                            setTypeface(null, Typeface.BOLD)
                            gravity = android.view.Gravity.CENTER_VERTICAL
                        }
                        adView.headlineView = headline

                        headerRow.addView(iconView)
                        headerRow.addView(headline)
                        rootLayout.addView(headerRow)

                        // Media View for Ad Image/Video
                        val mediaView = MediaView(ctx).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                480
                            )
                        }
                        adView.mediaView = mediaView
                        rootLayout.addView(mediaView)

                        // Body
                        val body = TextView(ctx).apply {
                            textSize = 14f
                            setTextColor(Color.parseColor("#CCCCCC"))
                            setPadding(0, 16, 0, 16)
                            maxLines = 3
                            gravity = android.view.Gravity.CENTER
                        }
                        adView.bodyView = body
                        rootLayout.addView(body)

                        // CTA Button with custom gradient/shape
                        val ctaBg = GradientDrawable().apply {
                            setColor(Color.parseColor("#7C4DFF"))
                            cornerRadius = 24f
                        }
                        val cta = Button(ctx).apply {
                            textSize = 15f
                            background = ctaBg
                            setTextColor(Color.WHITE)
                            setTypeface(null, Typeface.BOLD)
                            setPadding(32, 12, 32, 12)
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
                            it.text = ad.callToAction ?: "Visit Sponsor"
                            it.visibility = if (ad.callToAction != null) View.VISIBLE else View.GONE
                        }
                        adView.setNativeAd(ad)
                    }
                )

                // Bottom Soothing Prompt
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        "Swipe to continue reading",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ComposeColor.LightGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = ComposeColor.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
