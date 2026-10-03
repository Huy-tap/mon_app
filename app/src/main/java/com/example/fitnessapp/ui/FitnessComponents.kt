package com.example.fitnessapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.model.*
import java.time.LocalDate
import java.time.YearMonth

import android.graphics.drawable.PictureDrawable
import android.widget.ImageView
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.selection.selectable

enum class BottomTab(val title: String) {
    HOME("Trang chủ"), EXERCISES("Bài tập"), HISTORY("Lịch sử"), STATS("Thống kê"), SETTINGS("Cài đặt")
}
@Composable fun Label(text: String, size: Int = 14, bold: Boolean = false, muted: Boolean = false, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(text, modifier, color = if (color != Color.Unspecified) color else if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        fontSize = size.sp, letterSpacing = 0.sp, lineHeight = (size * 1.4).sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
}
@Composable fun FigmaIcon(file: String, modifier: Modifier = Modifier.size(24.dp)) {
    val context = LocalContext.current
    val picture = remember(file) { context.assets.open("figma/$file").use { SVG.getFromInputStream(it).renderToPicture() } }
    AndroidView(modifier = modifier, factory = { ImageView(it).apply {
        setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
    } }, update = { it.setImageDrawable(PictureDrawable(picture)) })
}
@Composable fun FitnessBottomNavigation(selectedTab: BottomTab, onTabSelected: (BottomTab) -> Unit) {
    val dark = MaterialTheme.colorScheme.background == Color(0xFF1C1B1F)
    val files = when {
        dark -> listOf("ad0de.svg", "ce785.svg", "7d27d.svg", "e6548.svg", "e307c.svg")
        selectedTab == BottomTab.SETTINGS -> listOf("04aea.svg", "2f6cd.svg", "74755.svg", "ee2a5.svg", "settings_selected.svg")
        selectedTab == BottomTab.STATS -> listOf("04aea.svg", "2f6cd.svg", "74755.svg", "statistics_selected.svg", "1519d.svg")
        else -> listOf(if (selectedTab == BottomTab.HOME) "190c1.svg" else "04aea.svg",
            if (selectedTab == BottomTab.EXERCISES) "c6120.svg" else "2f6cd.svg",
            if (selectedTab == BottomTab.HISTORY) "16b4a.svg" else "74755.svg", "ee2a5.svg", "1519d.svg")
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            BottomTab.entries.forEachIndexed { index, tab ->
                Column(Modifier.weight(1f).selectable(selectedTab == tab, onClick = { onTabSelected(tab) }, role = Role.Tab).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FigmaIcon(files[index], Modifier.size(24.dp))
                    Label(tab.title, 11, selectedTab == tab, color = if (selectedTab == tab) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
@Composable fun Page(title: String, onBack: (() -> Unit)? = null, tab: BottomTab? = null, onTab: (BottomTab) -> Unit = {},
    action: (@Composable RowScope.() -> Unit)? = null, footer: (@Composable () -> Unit)? = null, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { Row(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                Box(Modifier.size(24.dp, 48.dp).clickable(onClick = onBack).semantics { contentDescription = "Quay lại" }, contentAlignment = Alignment.CenterStart) { Label("‹", 28) }
            }
            Label(title, when (title) { "Nhắc nhở tập luyện" -> 20; "Nhập kết quả", "Sửa kết quả", "Thêm bài tập", "Sửa bài tập" -> 22; else -> 24 }, true, modifier = Modifier.weight(1f))
            action?.invoke(this)
        } }, bottomBar = {
            if (tab != null) FitnessBottomNavigation(tab, onTab)
            else if (footer != null) Surface(color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 20.dp, vertical = 12.dp)) { footer() }
            }
        }, content = content)
}
@Composable fun Panel(modifier: Modifier = Modifier, border: Boolean = true, padding: Int = 14, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
        border = if (border) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null) {
        Column(Modifier.padding(padding.dp), content = content)
    }
}
@Composable fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick, Modifier.fillMaxWidth().height(48.dp), enabled = enabled, shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable fun MonthSelector(month: YearMonth, onChange: (YearMonth) -> Unit) {
    var choose by remember { mutableStateOf(false) }
    Panel(padding = 0) {
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(month.minusMonths(1)) }, modifier = Modifier.semantics { contentDescription = "Tháng trước" }) { FigmaIcon("8c2e3.svg", Modifier.size(18.dp)) }
            Box(Modifier.weight(1f).fillMaxHeight().clickable { choose = true }, contentAlignment = Alignment.Center) {
                Label("Tháng %02d/%04d".format(month.monthValue, month.year), 15, true)
            }
            IconButton(onClick = { onChange(month.plusMonths(1)) }, modifier = Modifier.semantics { contentDescription = "Tháng sau" }) { FigmaIcon("ba9dd.svg", Modifier.size(18.dp)) }
        }
    }
    if (choose) {
        var year by remember { mutableIntStateOf(month.year) }
        AlertDialog(onDismissRequest = { choose = false }, title = { Label("Chọn tháng", 20, true) }, text = {
            Column {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    TextButton({ year-- }) { Text("‹") }; Label(year.toString(), 18, true); TextButton({ year++ }) { Text("›") }
                }
                (0..3).forEach { row -> Row(Modifier.fillMaxWidth()) { (1..3).forEach { col ->
                    val m = row * 3 + col
                    TextButton({ onChange(YearMonth.of(year, m)); choose = false }, Modifier.weight(1f)) { Text("Tháng $m") }
                } } }
            }
        }, confirmButton = { TextButton({ choose = false }) { Text("Đóng") } })
    }
}
@Composable fun EmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 82.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp)), Alignment.Center) { Label("MINH HỌA", 12, muted = true) }
        Spacer(Modifier.height(20.dp)); Label(title, 18, true)
        Spacer(Modifier.height(6.dp)); Text(message, Modifier.padding(horizontal = 16.dp), fontSize = 14.sp, letterSpacing = 0.sp, lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
@Composable fun FormField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    numeric: Boolean = false, placeholder: String = "", error: String? = null, singleLine: Boolean = true, embeddedLabel: Boolean = false, outlined: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!embeddedLabel) Label(label, 13, true)
        val borderColor = if (error != null) MaterialTheme.colorScheme.error else if (outlined || embeddedLabel) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
        Column(Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(if (embeddedLabel) 12.dp else 8.dp))
            .border(1.dp, borderColor, RoundedCornerShape(if (embeddedLabel) 12.dp else 8.dp))
            .padding(horizontal = 12.dp, vertical = if (embeddedLabel) 8.dp else 0.dp)) {
        if (embeddedLabel) Label(label, 12)
        BasicTextField(value, onChange, modifier = Modifier.fillMaxWidth().testTag("field/$label")
            .heightIn(min = if (embeddedLabel) 30.dp else if (singleLine) 48.dp else 72.dp)
            .padding(vertical = if (embeddedLabel) 0.dp else 12.dp),
            singleLine = singleLine, textStyle = LocalTextStyle.current.copy(fontSize = (if (embeddedLabel) 16 else 14).sp, fontWeight = if (embeddedLabel) FontWeight.Bold else FontWeight.Normal, letterSpacing = 0.sp, color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            decorationBox = { field -> Box(contentAlignment = Alignment.CenterStart) { if (value.isEmpty()) Label(placeholder, 14, muted = true); field() } })
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)

    }
}
