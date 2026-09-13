package com.ninerouter.monitor.ui.solar

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import com.ninerouter.monitor.data.model.TopologyProvider
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class HandlePosition { TOP, BOTTOM, LEFT, RIGHT }

enum class EdgeStatus { IDLE, LAST, ERROR, ACTIVE }

data class LayoutNode(
    val id: String,
    val isRouter: Boolean,
    val provider: TopologyProvider?,
    val meta: ProviderMeta,
    val bounds: Rect,
    val angle: Double = 0.0
)

data class LayoutEdge(
    val id: String,
    val providerId: String,
    val sourceHandle: HandlePosition,
    val targetHandle: HandlePosition,
    val sourcePoint: Offset,
    val targetPoint: Offset,
    val controlPoint1: Offset,
    val controlPoint2: Offset,
    val status: EdgeStatus
) {
    fun toComposePath(): Path {
        val path = Path()
        path.moveTo(sourcePoint.x, sourcePoint.y)
        path.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            targetPoint.x, targetPoint.y
        )
        return path
    }

    fun toAndroidPath(): android.graphics.Path {
        val path = android.graphics.Path()
        path.moveTo(sourcePoint.x, sourcePoint.y)
        path.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            targetPoint.x, targetPoint.y
        )
        return path
    }
}

data class TopologyLayout(
    val routerNode: LayoutNode,
    val providerNodes: List<LayoutNode>,
    val edges: List<LayoutEdge>,
    val worldBounds: Rect,
    val rx: Float,
    val ry: Float
) {
    /**
     * Menghitung skala seragam dan offset pemusatan agar worldBounds
     * pas di viewport Canvas dengan padding (meniru fitView di ReactFlow).
     */
    fun computeFitTransform(viewportWidth: Float, viewportHeight: Float, paddingFraction: Float = 0.18f): Pair<Float, Offset> {
        if (viewportWidth <= 0f || viewportHeight <= 0f) return 1f to Offset.Zero

        val targetW = viewportWidth * (1f - paddingFraction * 2f)
        val targetH = viewportHeight * (1f - paddingFraction * 2f)

        val worldW = max(worldBounds.width, 100f)
        val worldH = max(worldBounds.height, 100f)

        val scale = min(targetW / worldW, targetH / worldH)
        val worldCenterX = worldBounds.left + worldBounds.width / 2f
        val worldCenterY = worldBounds.top + worldBounds.height / 2f

        val viewCenterX = viewportWidth / 2f
        val viewCenterY = viewportHeight / 2f

        val offsetX = viewCenterX - (worldCenterX * scale)
        val offsetY = viewCenterY - (worldCenterY * scale)

        return scale to Offset(offsetX, offsetY)
    }
}

object TopologyGeometry {
    const val NODE_WIDTH = 126f
    const val NODE_HEIGHT = 30f
    const val ROUTER_WIDTH = 110f
    const val ROUTER_HEIGHT = 36f
    const val NODE_GAP = 28f

    /**
     * Implementasi layout elips proporsional:
     * Menjamin keliling elips selalu cukup untuk menampung seluruh node
     * dengan jarak aman antar node (tanpa overlap).
     */
    fun build(
        providers: List<TopologyProvider>,
        activeProviders: Set<String>,
        lastProvider: String,
        errorProvider: String
    ): TopologyLayout {
        val count = providers.size
        val routerNode = LayoutNode(
            id = "router",
            isRouter = true,
            provider = null,
            meta = ProviderCatalog.get("router"),
            bounds = Rect(
                left = -ROUTER_WIDTH / 2f,
                top = -ROUTER_HEIGHT / 2f,
                right = ROUTER_WIDTH / 2f,
                bottom = ROUTER_HEIGHT / 2f
            )
        )

        if (count == 0) {
            val bounds = routerNode.bounds.inflate(40f)
            return TopologyLayout(
                routerNode = routerNode,
                providerNodes = emptyList(),
                edges = emptyList(),
                worldBounds = bounds,
                rx = 0f,
                ry = 0f
            )
        }

        // Hitung keliling minimum agar jarak antar titik >= NODE_WIDTH + NODE_GAP
        val minRx = ((NODE_WIDTH + NODE_GAP) * count) / (2f * PI.toFloat())
        val rx = max(250f, minRx)
        val ry = max(155f, rx * 0.60f)

        val providerNodes = mutableListOf<LayoutNode>()
        val edges = mutableListOf<LayoutEdge>()

        var minX = routerNode.bounds.left
        var maxX = routerNode.bounds.right
        var minY = routerNode.bounds.top
        var maxY = routerNode.bounds.bottom

        providers.forEachIndexed { i, p ->
            val pid = p.provider.lowercase().trim()
            val meta = ProviderCatalog.get(p.provider)

            // Sudut mulai dari atas (-PI/2), searah jarum jam
            val angle = -PI / 2.0 + (2.0 * PI * i) / count.toDouble()
            val cx = (rx * cos(angle)).toFloat()
            val cy = (ry * sin(angle)).toFloat()

            val nodeRect = Rect(
                left = cx - NODE_WIDTH / 2f,
                top = cy - NODE_HEIGHT / 2f,
                right = cx + NODE_WIDTH / 2f,
                bottom = cy + NODE_HEIGHT / 2f
            )

            providerNodes.add(
                LayoutNode(
                    id = "provider-${p.provider}",
                    isRouter = false,
                    provider = p,
                    meta = meta,
                    bounds = nodeRect,
                    angle = angle
                )
            )

            minX = min(minX, nodeRect.left)
            maxX = max(maxX, nodeRect.right)
            minY = min(minY, nodeRect.top)
            maxY = max(maxY, nodeRect.bottom)

            // Pemilihan handle persis aturan web ProviderTopology.js:321
            val (sourceHandle, targetHandle) = pickHandles(angle, cx)

            val sourcePt = getHandlePoint(routerNode.bounds, sourceHandle)
            val targetPt = getHandlePoint(nodeRect, targetHandle)

            val (cp1, cp2) = computeBezierControlPoints(sourcePt, targetPt, sourceHandle, targetHandle)

            val status = when {
                pid == errorProvider.lowercase() && errorProvider.isNotBlank() -> EdgeStatus.ERROR
                activeProviders.contains(pid) -> EdgeStatus.ACTIVE
                pid == lastProvider.lowercase() && lastProvider.isNotBlank() -> EdgeStatus.LAST
                else -> EdgeStatus.IDLE
            }

            edges.add(
                LayoutEdge(
                    id = "edge-$pid",
                    providerId = pid,
                    sourceHandle = sourceHandle,
                    targetHandle = targetHandle,
                    sourcePoint = sourcePt,
                    targetPoint = targetPt,
                    controlPoint1 = cp1,
                    controlPoint2 = cp2,
                    status = status
                )
            )
        }

        val worldBounds = Rect(minX, minY, maxX, maxY).inflate(30f)

        return TopologyLayout(
            routerNode = routerNode,
            providerNodes = providerNodes,
            edges = edges,
            worldBounds = worldBounds,
            rx = rx,
            ry = ry
        )
    }

    private fun pickHandles(angle: Double, cx: Float): Pair<HandlePosition, HandlePosition> {
        val topThreshold = PI / 4.0
        val bottomThreshold = PI / 4.0

        return when {
            abs(angle + PI / 2.0) < topThreshold || abs(angle - 3.0 * PI / 2.0) < topThreshold -> {
                HandlePosition.TOP to HandlePosition.BOTTOM
            }
            abs(angle - PI / 2.0) < bottomThreshold -> {
                HandlePosition.BOTTOM to HandlePosition.TOP
            }
            cx > 0f -> {
                HandlePosition.RIGHT to HandlePosition.LEFT
            }
            else -> {
                HandlePosition.LEFT to HandlePosition.RIGHT
            }
        }
    }

    private fun getHandlePoint(rect: Rect, handle: HandlePosition): Offset {
        return when (handle) {
            HandlePosition.TOP -> Offset(rect.left + rect.width / 2f, rect.top)
            HandlePosition.BOTTOM -> Offset(rect.left + rect.width / 2f, rect.bottom)
            HandlePosition.LEFT -> Offset(rect.left, rect.top + rect.height / 2f)
            HandlePosition.RIGHT -> Offset(rect.right, rect.top + rect.height / 2f)
        }
    }

    private fun computeBezierControlPoints(
        source: Offset,
        target: Offset,
        sourceHandle: HandlePosition,
        targetHandle: HandlePosition
    ): Pair<Offset, Offset> {
        val dx = abs(target.x - source.x)
        val dy = abs(target.y - source.y)
        val offset = max(dx, dy) * 0.5f

        val cp1 = when (sourceHandle) {
            HandlePosition.TOP -> Offset(source.x, source.y - offset)
            HandlePosition.BOTTOM -> Offset(source.x, source.y + offset)
            HandlePosition.LEFT -> Offset(source.x - offset, source.y)
            HandlePosition.RIGHT -> Offset(source.x + offset, source.y)
        }

        val cp2 = when (targetHandle) {
            HandlePosition.TOP -> Offset(target.x, target.y - offset)
            HandlePosition.BOTTOM -> Offset(target.x, target.y + offset)
            HandlePosition.LEFT -> Offset(target.x - offset, target.y)
            HandlePosition.RIGHT -> Offset(target.x + offset, target.y)
        }

        return cp1 to cp2
    }
}
