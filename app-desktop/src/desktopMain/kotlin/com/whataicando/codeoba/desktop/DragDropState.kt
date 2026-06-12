package com.whataicando.codeoba.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.whataicando.codeoba.core.domain.model.Session

class DragDropState {
    var draggingSession by mutableStateOf<Session?>(null)
    var draggingTag by mutableStateOf<Pair<Session, String>?>(null) // Session to Tag Name
    var dragPosition by mutableStateOf(Offset.Zero)
    var hoveredGroupByName by mutableStateOf<String?>(null)
    var isHoveringRemoveZone by mutableStateOf(false)

    // Bounds of all potential drop targets (populated during layout, relative to root)
    val dropTargetBounds = mutableStateMapOf<String, Rect>()
    var removeZoneBounds by mutableStateOf<Rect?>(null)
    var detailHeaderBounds by mutableStateOf<Rect?>(null)

    fun reset() {
        draggingSession = null
        draggingTag = null
        dragPosition = Offset.Zero
        hoveredGroupByName = null
        isHoveringRemoveZone = false
    }

    fun updateDragPosition(position: Offset) {
        dragPosition = position
        
        // Update hovered group
        hoveredGroupByName = dropTargetBounds.entries.firstOrNull { it.value.contains(position) }?.key
        
        // Update remove zone hovering
        isHoveringRemoveZone = removeZoneBounds?.contains(position) ?: false
    }
}
