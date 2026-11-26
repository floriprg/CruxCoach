package com.cruxcoach.android.ui.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cruxcoach.android.R
import com.cruxcoach.android.data.GradeScale
import com.cruxcoach.android.ui.theme.OrangeAccent
import com.cruxcoach.android.util.GradeDisplayHelper
import com.cruxcoach.data.repository.ClimbWithStats
import com.cruxcoach.domain.board.BoulderSide

/**
 * Compact strip shown above the board when two horizontally disjoint boulders
 * are displayed simultaneously. Renders Left and Right pills matching their
 * physical positions on the wall.
 */
@Composable
internal fun DualBoulderStrip(
    primaryClimb: ClimbWithStats,
    paired: PairedBoulderState,
    gradeScale: GradeScale,
    onChangePaired: () -> Unit,
    onRemovePaired: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val leftIsPaired = paired.side == BoulderSide.LEFT
    val leftClimb = if (leftIsPaired) paired.climb else primaryClimb
    val rightClimb = if (leftIsPaired) primaryClimb else paired.climb

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dual_boulder_strip"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DualBoulderSidePill(
                sideLabel = stringResource(R.string.dual_boulder_side_left_short),
                climb = leftClimb,
                gradeScale = gradeScale,
                isPaired = leftIsPaired,
                onClick = if (leftIsPaired) onChangePaired else null,
                modifier = Modifier.weight(1f),
            )
            DualBoulderSidePill(
                sideLabel = stringResource(R.string.dual_boulder_side_right_short),
                climb = rightClimb,
                gradeScale = gradeScale,
                isPaired = !leftIsPaired,
                onClick = if (!leftIsPaired) onChangePaired else null,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onRemovePaired,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("dual_boulder_remove_button"),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.dual_boulder_remove),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DualBoulderSidePill(
    sideLabel: String,
    climb: ClimbWithStats,
    gradeScale: GradeScale,
    isPaired: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val grade = climb.difficultyAverage?.let { GradeDisplayHelper.formatDifficulty(it, gradeScale) }
    val bgColor = if (isPaired) {
        OrangeAccent.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }
    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = sideLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isPaired) OrangeAccent else MaterialTheme.colorScheme.primary,
        )
        Text(
            text = climb.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isPaired) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (grade != null) {
            Text(
                text = grade,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Modal bottom sheet listing only boulders that are strictly horizontally
 * disjoint from the current boulder (no horizontal overlap).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DualBoulderPickerSheet(
    pickerState: DualBoulderPickerState,
    gradeScale: GradeScale,
    onQueryChange: (String) -> Unit,
    onSelect: (ClimbWithStats) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("dual_boulder_picker_sheet"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.dual_boulder_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!pickerState.isLoading && pickerState.totalCompatible > 0) {
                Text(
                    text = stringResource(
                        R.string.dual_boulder_picker_subtitle,
                        pickerState.totalCompatible,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(
                value = pickerState.query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = {
                    Text(stringResource(R.string.dual_boulder_picker_search_placeholder))
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = if (pickerState.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dual_boulder_search_input"),
                shape = RoundedCornerShape(12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 420.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    pickerState.isLoading -> {
                        CircularProgressIndicator(color = OrangeAccent)
                    }
                    pickerState.candidates.isEmpty() -> {
                        val emptyText = if (pickerState.query.isNotBlank()) {
                            stringResource(
                                R.string.dual_boulder_picker_no_search_results,
                                pickerState.query.trim(),
                            )
                        } else {
                            stringResource(R.string.dual_boulder_picker_empty)
                        }
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            items(
                                items = pickerState.candidates,
                                key = { it.climb.uuid },
                            ) { candidate ->
                                DualBoulderCandidateRow(
                                    candidate = candidate,
                                    gradeScale = gradeScale,
                                    onClick = { onSelect(candidate.climb) },
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DualBoulderCandidateRow(
    candidate: DualBoulderCandidate,
    gradeScale: GradeScale,
    onClick: () -> Unit,
) {
    val climb = candidate.climb
    val grade = climb.difficultyAverage?.let { GradeDisplayHelper.formatDifficulty(it, gradeScale) }
    val sideText = stringResource(
        if (candidate.side == BoulderSide.LEFT) R.string.dual_boulder_side_left
        else R.string.dual_boulder_side_right
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = OrangeAccent.copy(alpha = 0.14f),
        ) {
            Text(
                text = sideText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = OrangeAccent,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = climb.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val setter = climb.setterUsername
            if (!setter.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = setter,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (grade != null) {
            Text(
                text = grade,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
