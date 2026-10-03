package com.example.fitnessapp.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitnessapp.model.MonthlyStats
import java.time.YearMonth

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Error(val message: String) : StatsUiState
    data class Success(val month: YearMonth, val stats: MonthlyStats, val weeklyCounts: List<Int>) : StatsUiState
}

@Composable
fun StatsScreen(state: StatsUiState, month: YearMonth, onMonth: (YearMonth) -> Unit,
    onTab: (BottomTab) -> Unit, onReminder: () -> Unit, onRecord: () -> Unit, onRetry: () -> Unit) {
    ProvideTextStyle(LocalTextStyle.current.copy(fontFamily = StatsFont)) {
        Page("Thống kê", tab = BottomTab.STATS, onTab = onTab, titleSize = 22, action = {
            Surface(shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.size(40.dp).clickable(onClick = onReminder).semantics { contentDescription = "Nhắc nhở tập luyện" }, Alignment.Center) {
                    FigmaIcon("belldot.svg", Modifier.size(18.dp))
                }
            }
        }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MonthSelector(month, onMonth, bottomSheet = true)
                when {
                    state is StatsUiState.Loading || state is StatsUiState.Success && state.month != month ->
                        Box(Modifier.fillMaxWidth().height(180.dp).testTag("stats/loading"), Alignment.Center) { CircularProgressIndicator() }
                    state is StatsUiState.Error -> StatsMessage("Lỗi tải dữ liệu", state.message, "alerttriangle.svg", "Thử lại", onRetry)
                    state is StatsUiState.Success -> {
                        Label("Tháng ${month.monthValue} của bạn", 15, true)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(state.stats.totalWorkouts to "Buổi tập", state.stats.totalMinutes to "Phút tập", state.stats.completedExercises to "Lượt bài xong").forEach { (value, title) ->
                                Panel(Modifier.weight(1f).testTag("stats/$title"), padding = 16, radius = 16) {
                                    BasicText(value.toString(), Modifier.fillMaxWidth().testTag("stats/value/$title"), maxLines = 1,
                                        style = LocalTextStyle.current.copy(fontFamily = StatsFont, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = 0.sp,
                                            fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface),
                                        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 28.sp, stepSize = 1.sp))
                                    Spacer(Modifier.height(6.dp))
                                    BasicText(title, Modifier.fillMaxWidth().testTag("stats/label/$title"), maxLines = 1,
                                        style = LocalTextStyle.current.copy(fontFamily = StatsFont, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 11.sp, stepSize = 0.5.sp))
                                }
                            }
                        }
                        if (state.stats.totalWorkouts == 0) StatsMessage(null, "Chưa có dữ liệu tập luyện trong tháng này", "dumbbell.svg", "Bắt đầu tập luyện", onRecord)
                        else Panel(padding = 16, radius = 16) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Label("Tần suất tập luyện", 15, true, modifier = Modifier.weight(1f))
                                Label(if (month == YearMonth.now()) "Tháng này" else "%02d/%04d".format(month.monthValue, month.year), 12, muted = true)
                            }
                            Spacer(Modifier.height(16.dp))
                            val maximum = (state.weeklyCounts.maxOrNull() ?: 0).coerceAtLeast(1)
                            Row(Modifier.fillMaxWidth().heightIn(min = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Bottom) {
                                state.weeklyCounts.forEachIndexed { i, count ->
                                    Column(Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "Tuần ${i + 1}: $count buổi" }, horizontalAlignment = Alignment.CenterHorizontally) {
                                        Label(count.toString(), 11, true); Spacer(Modifier.height(8.dp))
                                        Box(Modifier.fillMaxWidth().height((48f * count / maximum).dp)
                                            .background(if (count == maximum) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)))
                                        Spacer(Modifier.height(8.dp)); Label("Tuần ${i + 1}", 11, muted = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun StatsMessage(title: String?, message: String, icon: String, button: String, onClick: () -> Unit) {
    Panel(padding = 40, radius = 20) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(36.dp)), Alignment.Center) { FigmaIcon(icon, Modifier.size(36.dp)) }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                title?.let { Label(it, 16, true) }
                Text(message, Modifier.fillMaxWidth(), fontSize = 14.sp, fontFamily = StatsFont, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            PrimaryButton(button, onClick = onClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun MonthPickerSheet(month: YearMonth, onDismiss: () -> Unit, onConfirm: (YearMonth) -> Unit) {
    var year by rememberSaveable { mutableIntStateOf(month.year) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(month.monthValue) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 8.dp, bottom = 20.dp).size(40.dp, 4.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp))) }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Label("Chọn thời gian", 18, true, modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                IconButton({ if (year > 1) year-- }, Modifier.semantics { contentDescription = "Năm trước" }) { FigmaIcon("8c2e3.svg", Modifier.size(16.dp)) }
                Label("Năm $year", 15, true)
                IconButton({ if (year < 9999) year++ }, Modifier.semantics { contentDescription = "Năm sau" }) { FigmaIcon("ba9dd.svg", Modifier.size(16.dp)) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..3).forEach { col ->
                        val m = row * 3 + col
                        val selected = m == selectedMonth
                        Box(Modifier.weight(1f).height(44.dp).background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .selectable(selected, onClick = { selectedMonth = m }, role = Role.RadioButton).testTag("month/$m"), Alignment.Center) {
                            Label("Tháng %02d".format(m), 14, selected, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onDismiss, Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Hủy") }
                Box(Modifier.weight(1f)) { PrimaryButton("Xác nhận") { onConfirm(YearMonth.of(year, selectedMonth)) } }
            }
        }
    }
}
