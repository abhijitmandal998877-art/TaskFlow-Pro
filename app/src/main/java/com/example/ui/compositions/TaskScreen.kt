package com.example.ui.compositions

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LongTermGoal
import com.example.data.Subtask
import com.example.data.Task
import com.example.ui.viewmodel.TaskViewModel
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt

// Priority mapper defined locally
enum class PriorityLevel(val code: Int, val label: String, val color: Color) {
    LOW(0, "LOW", Color(0xFF10B981)),
    MEDIUM(1, "MEDIUM", Color(0xFFF59E0B)),
    HIGH(2, "HIGH", Color(0xFFEF4444))
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var priorityFilter by remember { mutableStateOf<Int?>(null) } // null = All, 0 = Low, 1 = Medium, 2 = High

    // Fit tasks with priority levels locally
    val filteredTasks = remember(tasks, priorityFilter) {
        if (priorityFilter == null) {
            tasks
        } else {
            tasks.filter { it.priority == priorityFilter }
        }
    }

    // Aggregate progress stats
    val totalCount = tasks.size
    val completedCount = tasks.count { it.isCompleted }
    val progressFraction = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    Scaffold(
        modifier = modifier.testTag("task_screen_root"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // -- TOP HEADER BLOCK --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TaskFlow Pro",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Orchestrate your day with flow.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Smooth Theme Switch & Sync actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle color scheme Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Simulated iCloud sync action
                    IconButton(
                        onClick = { viewModel.triggerCloudSync(context) },
                        enabled = !isSyncing,
                        modifier = Modifier.testTag("icloud_sync_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Simulate Cloud Storage Synced",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- INTEGRATED PROGRESS HUD CONTAINER CARD --
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Orchestration Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        // iCloud status label
                        Text(
                            text = if (isSyncing) "Syncing..." else "Synced to iCloud",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${(progressFraction * 100).roundToInt()}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "$completedCount of $totalCount task milestones met.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // -- DYNAMIC PRIORITY HORIZONTAL FILTER BAR --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = priorityFilter == null,
                    onClick = { priorityFilter = null },
                    label = { Text("All Priorities") },
                    leadingIcon = { Icon(Icons.Default.List, "All", modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("filter_chip_all")
                )

                PriorityLevel.values().forEach { level ->
                    val isSel = priorityFilter == level.code
                    FilterChip(
                        selected = isSel,
                        onClick = { priorityFilter = level.code },
                        label = { Text(level.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = level.label,
                                tint = level.color,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("filter_chip_${level.label.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -- DYNAMIC TASKS LIST SECTIONS --
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.AssignmentLate,
                            contentDescription = "Empty Desk",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Workspace is clear!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add milestone tasks, schedule reminders, and track subtasks smoothly.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                // List of tasks displaying re-ordering gestures
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(filteredTasks, key = { _, task -> task.id }) { index, task ->
                        // State for reorder-drag positions
                        var offsetTransitionY by remember { mutableStateOf(0f) }
                        var isDragging by remember { mutableStateOf(false) }

                        val dragModifier = Modifier.pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    // Trigger simple relative swaps based on boundaries
                                    if (offsetTransitionY > 150f && index < filteredTasks.size - 1) {
                                        viewModel.moveTask(index, index + 1, context)
                                        Toast.makeText(context, "Position swapped downwards!", Toast.LENGTH_SHORT).show()
                                    } else if (offsetTransitionY < -150f && index > 0) {
                                        viewModel.moveTask(index, index - 1, context)
                                        Toast.makeText(context, "Position swapped upwards!", Toast.LENGTH_SHORT).show()
                                    }
                                    offsetTransitionY = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    offsetTransitionY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    offsetTransitionY += dragAmount.y
                                }
                            )
                        }

                        TaskFlowMilestoneCard(
                            task = task,
                            linkedGoal = goals.find { it.id == task.longTermGoalId },
                            isDragging = isDragging,
                            offsetY = offsetTransitionY,
                            dragModifier = dragModifier,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            goals = goals,
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, desc, prioCode, goalId, reminderMs ->
                viewModel.addTask(context, title, desc, prioCode, null, reminderMs, goalId)
                showAddTaskDialog = false
                Toast.makeText(context, "New priority milestone set!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun TaskFlowMilestoneCard(
    task: Task,
    linkedGoal: LongTermGoal?,
    isDragging: Boolean,
    offsetY: Float,
    dragModifier: Modifier,
    viewModel: TaskViewModel
) {
    val context = LocalContext.current
    var isSubtasksExpanded by remember { mutableStateOf(false) }
    var showAddSubtaskDialog by remember { mutableStateOf(false) }

    // Query subtasks dynamically inside each card
    val subtasksState = viewModel.getSubtasks(task.id).collectAsState(initial = emptyList())
    val subtasks = subtasksState.value

    // Display dragging visual elevations
    val elevationState = animateDpAsState(
        targetValue = if (isDragging) 16.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "elevationTransition"
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragModifier)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .shadow(elevationState.value, shape = RoundedCornerShape(16.dp))
            .testTag("task_card_${task.id}"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            // Main structure row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Complete checkbox status indicator
                IconButton(
                    onClick = {
                        viewModel.toggleTaskCompletion(context, task)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.TaskAlt else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Task status",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )

                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    // Metadata labels
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val prioLevel = PriorityLevel.values().find { it.code == task.priority } ?: PriorityLevel.MEDIUM
                        
                        // Priority Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(prioLevel.color.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = prioLevel.label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = prioLevel.color
                            )
                        }

                        // Connected LongTerm goal
                        if (linkedGoal != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Goal: ${linkedGoal.title}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }

                        // Reminder Alarm Alert label
                        if (task.reminderTime != null) {
                            val rStr = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(task.reminderTime))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Alert on",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = rStr,
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Drag indicator Handle + delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            viewModel.deleteTask(context, task)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Task milestone",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Hold & Drag to pivot position",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 4.dp)
                    )
                }
            }

            // Expand Subtasks Area trigger row
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isSubtasksExpanded = !isSubtasksExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val compSub = subtasks.count { it.isCompleted }
                val totSub = subtasks.size
                Text(
                    text = "Subtasks Checklist ($compSub/$totSub completed)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { showAddSubtaskDialog = true },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, "Add subtask", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Subtask", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Icon(
                        imageVector = if (isSubtasksExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand checklist",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Animating expandable checklist block
            AnimatedVisibility(
                visible = isSubtasksExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (subtasks.isEmpty()) {
                        Text(
                            text = "No subtask targets declared. Add a subtask to partition workflows.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    } else {
                        subtasks.forEach { sub ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = sub.isCompleted,
                                    onCheckedChange = { viewModel.toggleSubtaskCompletion(sub) },
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sub.title,
                                    fontSize = 12.sp,
                                    color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (sub.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteSubtask(sub) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete subtask",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSubtaskDialog) {
        AddSubtaskDialog(
            onDismiss = { showAddSubtaskDialog = false },
            onConfirm = { title ->
                viewModel.addSubtask(task.id, title)
                showAddSubtaskDialog = false
                isSubtasksExpanded = true
                Toast.makeText(context, "Subtask scheduled!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun AddSubtaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtask Milestone", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Subtask Description") },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("dialog_subtask_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onConfirm(title)
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("dialog_subtask_confirm")
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun AddTaskDialog(
    goals: List<LongTermGoal>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, priorityCode: Int, goalId: Int?, reminderMs: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priorityCode by remember { mutableStateOf(1) } // Default 1 = Medium
    var selectedGoalId by remember { mutableStateOf<Int?>(null) }
    var reminderMs by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Establish Milestone",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Milestone Title (e.g. Design UI)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("dialog_task_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Task Details") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Priority Selection
                Text("Priority Level:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityLevel.values().forEach { level ->
                        val isSelected = priorityCode == level.code
                        ElevatedButton(
                            onClick = { priorityCode = level.code },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.weight(1f).testTag("dialog_prio_${level.label.lowercase()}"),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(level.label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Goal Connection Selection
                if (goals.isNotEmpty()) {
                    Text("Connect to Long-term Goal:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    var expandedDropdown by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expandedDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedGoalId != null) {
                                    goals.find { it.id == selectedGoalId }?.title ?: "Select Goal"
                                } else {
                                    "No Linked Aspiration"
                                },
                                fontSize = 12.sp
                            )
                        }

                        DropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedGoalId = null
                                    expandedDropdown = false
                                }
                            )
                            goals.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        selectedGoalId = g.id
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Customizable Reminder Picker Dialogue trigger
                Text("Customizable Reminder Trigger:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val sel = Calendar.getInstance()
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            sel.set(year, month, day, hour, minute)
                                            reminderMs = sel.timeInMillis
                                        },
                                        now.get(Calendar.HOUR_OF_DAY),
                                        now.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH),
                                now.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    ) {
                        Icon(Icons.Default.Alarm, contentDescription = "Alarm trigger selector")
                    }

                    Text(
                        text = if (reminderMs != null) {
                            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(reminderMs!!))
                        } else {
                            "No alarm active"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).padding(start = 12.dp)
                    )

                    if (reminderMs != null) {
                        IconButton(onClick = { reminderMs = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancel Reminder Alert", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, desc, priorityCode, selectedGoalId, reminderMs)
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("dialog_task_confirm")
            ) {
                Text("Establish")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}
