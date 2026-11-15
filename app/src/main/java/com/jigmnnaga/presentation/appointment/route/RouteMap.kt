package com.jigmnnaga.presentation.appointment.route

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jigmnnaga.R
import com.jigmnnaga.core.designsystem.theme.AppColors

// Figma 지도 프레임(412dp 폭)의 좌표를 그대로 쓰고, 가로는 실제 폭에 맞춰 늘이거나 줄인다.
private const val MapDesignWidth = 412f
internal val RouteMapHeight = 248.dp
internal val RouteMapOverviewHeight = 190.dp

@DrawableRes
internal fun RouteTransport.iconRes(): Int = when (this) {
    RouteTransport.BUS -> R.drawable.bus
    RouteTransport.SUBWAY -> R.drawable.subway
    RouteTransport.TAXI -> R.drawable.car
}

@DrawableRes
internal fun RouteStepKind.iconRes(): Int = when (this) {
    RouteStepKind.START -> R.drawable.my_location
    RouteStepKind.WALK -> R.drawable.walking
    RouteStepKind.BUS -> R.drawable.bus
    RouteStepKind.SUBWAY -> R.drawable.subway
    RouteStepKind.TAXI -> R.drawable.car
    RouteStepKind.ARRIVAL -> R.drawable.place
}

private fun Modifier.at(x: Float, y: Float, scale: Float): Modifier =
    offset(x = (x * scale).dp, y = y.dp)

private fun DrawScope.designPoint(x: Float, y: Float): Offset {
    val scale = size.width / (MapDesignWidth * density)
    return Offset(x * density * scale, y * density)
}

/** 블록·공원·도로만 그린 배경. 실제 지도 SDK가 붙으면 이 자리를 지도가 대신한다. */
private fun DrawScope.drawCityBase() {
    val scale = size.width / (MapDesignWidth * density)
    drawRect(AppColors.Monotone.Gray100)

    val blockXs = listOf(-18f, 94f, 206f, 318f)
    val blockYs = listOf(-28f, 63f, 154f, 245f, 336f)
    blockYs.forEach { y ->
        blockXs.forEach { x ->
            drawRoundRect(
                color = AppColors.Monotone.Gray200,
                topLeft = designPoint(x, y),
                size = Size(91f * density * scale, 68f * density),
                cornerRadius = CornerRadius(7f * density)
            )
        }
    }

    // 큰 도로. 블록 사이 틈 가운데에 흰 띠를 깐다.
    val road = AppColors.Monotone.White
    drawRect(road, designPoint(0f, 128f), Size(size.width, 14f * density))
    drawRect(road, designPoint(0f, 200f), Size(size.width, 8f * density))
    drawRect(road, designPoint(109f, 0f), Size(14f * density * scale, size.height))
    drawRect(road, designPoint(197f, 0f), Size(6f * density * scale, size.height))
    drawRect(road, designPoint(286f, 0f), Size(8f * density * scale, size.height))

    // 근린공원
    drawRoundRect(
        color = AppColors.MainGreen.Green300,
        topLeft = designPoint(281f, 153f),
        size = Size(121f * density * scale, 94f * density),
        cornerRadius = CornerRadius(18f * density)
    )
    val parkPath = Path().apply {
        moveTo(designPoint(296f, 215f).x, designPoint(296f, 215f).y)
        cubicTo(
            designPoint(320f, 160f).x, designPoint(320f, 160f).y,
            designPoint(350f, 170f).x, designPoint(350f, 170f).y,
            designPoint(358f, 200f).x, designPoint(358f, 200f).y
        )
        cubicTo(
            designPoint(364f, 220f).x, designPoint(364f, 220f).y,
            designPoint(380f, 190f).x, designPoint(380f, 190f).y,
            designPoint(392f, 165f).x, designPoint(392f, 165f).y
        )
    }
    drawPath(
        path = parkPath,
        color = AppColors.Monotone.White,
        style = Stroke(width = 2f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawRoute(points: List<Offset>, color: Color, widthDp: Float, dotted: Boolean) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = widthDp * density,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = if (dotted) {
                PathEffect.dashPathEffect(floatArrayOf(1f * density, 4f * density))
            } else {
                null
            }
        )
    )
}

@Composable
private fun MapLabel(text: String, color: Color, fontSize: Int, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize.sp, lineHeight = (fontSize + 4).sp)
    )
}

/** 초기화면(ROU-001-01)의 작은 지도. 내 위치(노란 핀)에서 약속 장소(진한 핀)까지 점선을 잇는다. */
@Composable
internal fun RouteOverviewMap(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.route_map_area)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(RouteMapOverviewHeight)
            .clip(RoundedCornerShape(24.dp))
            .semantics { contentDescription = description }
            .testTag("route-map-overview")
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCityBase()
            drawRoute(
                points = listOf(
                    designPoint(98f, 112f),
                    designPoint(130f, 92f),
                    designPoint(168f, 88f),
                    designPoint(188f, 70f)
                ),
                color = AppColors.Monotone.Gray400,
                widthDp = 2f,
                dotted = true
            )
        }
        val k = maxWidth.value / MapDesignWidth
        MapPin(color = AppColors.Icon.Yellow, label = stringResource(R.string.route_me), modifier = Modifier.at(88f, 100f, k))
        MapPin(color = AppColors.MainGreen.Green900, label = null, modifier = Modifier.at(176f, 42f, k))
    }
}

@Composable
private fun MapPin(color: Color, label: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 24.dp, height = 30.dp), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) {
            val r = 11f * density
            drawCircle(color = color, radius = r, center = Offset(size.width / 2, r))
            val tail = Path().apply {
                moveTo(size.width / 2 - 6f * density, r + 6f * density)
                lineTo(size.width / 2, size.height)
                lineTo(size.width / 2 + 6f * density, r + 6f * density)
                close()
            }
            drawPath(tail, color)
            drawCircle(color = Color.White, radius = 4f * density, center = Offset(size.width / 2, r))
        }
        if (label != null) {
            Text(
                text = label,
                modifier = Modifier.padding(top = 4.dp),
                color = color,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

/** 경로 선택·약속 이동 단계의 지도. 단계에 따라 경로 색, 내 위치 배지, 친구 배지가 달라진다. */
@Composable
internal fun RouteMap(
    state: RouteUiState,
    onMyLocationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = stringResource(R.string.route_map_area)
    val moving = state.moving?.takeIf { state.stage == RouteStage.MOVING }
    val arrived = moving?.hasArrived == true
    val badgeTransport = when {
        moving != null -> moving.myTransport
        else -> state.expandedTransport
            ?: state.selectedTransport
            ?: state.options.firstOrNull()?.transport
            ?: RouteTransport.BUS
    }
    val routeColor = if (arrived) AppColors.Monotone.Gray300 else AppColors.MainGreen.Green700

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(RouteMapHeight)
            .semantics { contentDescription = description }
            .testTag("route-map")
    ) {
        val k = maxWidth.value / MapDesignWidth
        Canvas(Modifier.fillMaxSize()) {
            drawCityBase()
            // 걷는 구간은 점선, 탈것 구간은 실선으로 그린다.
            drawRoute(
                points = listOf(
                    designPoint(84f, 170f), designPoint(116f, 170f), designPoint(116f, 158f)
                ),
                color = routeColor,
                widthDp = 3f,
                dotted = true
            )
            drawRoute(
                points = listOf(
                    designPoint(116f, 158f), designPoint(116f, 124f),
                    designPoint(274f, 124f), designPoint(274f, 100f)
                ),
                color = routeColor,
                widthDp = 5f,
                dotted = false
            )
            drawRoute(
                points = listOf(
                    designPoint(274f, 100f), designPoint(274f, 84f), designPoint(318f, 84f)
                ),
                color = routeColor,
                widthDp = 3f,
                dotted = true
            )
        }
        MapLabel("역삼동", AppColors.Monotone.Gray600, 11, Modifier.at(24f, 47f, k))
        MapLabel("테헤란로", AppColors.Monotone.Gray600, 10, Modifier.at(151f, 143f, k))
        MapLabel("근린공원", AppColors.MainGreen.Green900, 10, Modifier.at(306f, 194f, k))

        RoutePoint(Modifier.at(111f, 153f, k))
        RoutePoint(Modifier.at(269f, 95f, k))

        if (moving == null) {
            TransportWaypoint(badgeTransport, Modifier.at(182f, 108f, k))
        }

        DestinationMarker(state.destinationName, Modifier.at(300f, 42f, k), labelModifier = Modifier.at(224f, 16f, k))

        when {
            moving == null -> MyLocationBadge(
                iconRes = R.drawable.my_location,
                text = stringResource(R.string.route_current_location),
                modifier = Modifier.at(21f, 132f, k)
            )
            arrived -> ArrivedBadge(Modifier.at(272f, 88f, k))
            else -> MyLocationBadge(
                iconRes = moving.myTransport.iconRes(),
                text = moving.myMapLabel,
                modifier = Modifier.at(130f, 78f, k)
            )
        }

        if (moving != null) {
            state.members.filter { !it.isMe }.forEachIndexed { index, member ->
                val transport = member.transport ?: return@forEachIndexed
                val offset = if (index == 0) Offset(24f, 12f) else Offset(224f, 184f)
                FriendChip(
                    text = "${member.name} · ${transport.shortLabel()}",
                    iconRes = transport.iconRes(),
                    modifier = Modifier.at(offset.x, offset.y, k)
                )
            }
        }

        MyLocationButton(
            onClick = onMyLocationClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 12.dp)
        )
    }
}

@Composable
private fun RouteTransport.shortLabel(): String = when (this) {
    RouteTransport.BUS -> stringResource(R.string.route_transport_bus)
    RouteTransport.SUBWAY -> stringResource(R.string.route_transport_subway)
    RouteTransport.TAXI -> stringResource(R.string.route_transport_taxi)
}

@Composable
private fun RoutePoint(modifier: Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(Color.White, CircleShape)
            .border(2.dp, AppColors.MainGreen.Green700, CircleShape)
    )
}

@Composable
private fun TransportWaypoint(transport: RouteTransport, modifier: Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .background(AppColors.MainGreen.Green700, RoundedCornerShape(9.dp))
            .border(2.dp, Color.White, RoundedCornerShape(9.dp))
            .testTag("route-map-waypoint"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(transport.iconRes()),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun DestinationMarker(name: String, modifier: Modifier, labelModifier: Modifier) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 40.dp)
            .background(AppColors.MainGreen.Green900, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.place),
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color.White
        )
    }
    if (name.isNotEmpty()) {
        Text(
            text = name,
            modifier = labelModifier
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp),
            color = AppColors.Monotone.Gray900,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 16.sp
            )
        )
    }
}

@Composable
private fun MyLocationBadge(@DrawableRes iconRes: Int, text: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .shadow(6.dp, CircleShape, clip = false)
                .background(AppColors.MainGreen.Green700, CircleShape)
                .border(2.dp, Color.White, CircleShape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(AppColors.MainGreen.Green700, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.route_me),
                    color = AppColors.MainGreen.Green200,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = Color.White
            )
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(AppColors.MainGreen.Green700, CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun ArrivedBadge(modifier: Modifier) {
    Text(
        text = stringResource(R.string.route_badge_arrived),
        modifier = modifier
            .background(AppColors.MainGreen.Green900, CircleShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
private fun FriendChip(text: String, @DrawableRes iconRes: Int, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, AppColors.MainGreen.Green400, RoundedCornerShape(14.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.MainGreen.Green700
        )
        Text(
            text = text,
            color = AppColors.MainGreen.Green700,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
private fun MyLocationButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.route_my_location_button)
    Box(
        modifier = modifier
            .size(38.dp)
            .shadow(4.dp, CircleShape, clip = false)
            .background(Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .testTag("route-my-location-button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.my_location),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.MainGreen.Green700
        )
    }
}
