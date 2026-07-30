package com.edalxgoam.nrxgoam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FinanceBlue = Color(0xFF061A79)
private val FinanceBrightBlue = Color(0xFF0618AD)
private val FinanceSky = Color(0xFF56ACEF)
private val FinanceAqua = Color(0xFF75D5DF)
private val FinanceInk = Color(0xFF061044)
private val FinanceBackground = Color(0xFFF4F6F6)
private val FinanceCard = Color(0xFFFFFFFF)
private val FinanceMuted = Color(0xFFEEF1F1)

private enum class PrototypeDestination { Home, Opportunities, CardDetail, Movements }

@Composable
fun FinancePrototypeApp() {
    var destination by remember { mutableStateOf(PrototypeDestination.Home) }
    BackHandler(destination != PrototypeDestination.Home) {
        destination = PrototypeDestination.Home
    }

    Surface(color = FinanceBackground, modifier = Modifier.fillMaxSize()) {
        when (destination) {
            PrototypeDestination.Home -> FinanceHomeScreen(
                onOpenOpportunities = { destination = PrototypeDestination.Opportunities },
                onOpenCard = { destination = PrototypeDestination.CardDetail },
            )
            PrototypeDestination.Opportunities -> OpportunitiesScreen(
                onHome = { destination = PrototypeDestination.Home },
            )
            PrototypeDestination.CardDetail -> CardDetailScreen(
                onBack = { destination = PrototypeDestination.Home },
                onMovements = { destination = PrototypeDestination.Movements },
            )
            PrototypeDestination.Movements -> MovementsScreen(
                onBack = { destination = PrototypeDestination.CardDetail },
            )
        }
    }
}

@Composable
private fun FinanceHomeScreen(
    onOpenOpportunities: () -> Unit,
    onOpenCard: () -> Unit,
) {
    var productTab by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PrototypeTopBar("Inicio") }
            item { SegmentedTabs(productTab, onSelected = { productTab = it }) }
            if (productTab == 0) {
                item { FinancialHealthCard() }
                item { SectionTitle("Principales productos", editable = true) }
                item { AccountProductCard(onOpenCard) }
                item { QuickActionsSection() }
                item { CoachCard() }
                item { SectionTitle("Te podría interesar") }
                item { InterestCards(onOpenOpportunities) }
            } else {
                item { ProductsList(onOpenCard) }
            }
        }
        BottomNavigationBar(
            selected = 0,
            modifier = Modifier.align(Alignment.BottomCenter),
            onHome = {},
            onOpportunities = onOpenOpportunities,
        )
    }
}

@Composable
private fun PrototypeTopBar(title: String, back: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (back == null) {
            TopAction(Icons.Default.MoreVert, "Visible")
        } else {
            TopAction(Icons.AutoMirrored.Filled.ArrowBack, "Volver", back)
        }
        Text(
            text = title,
            color = FinanceInk,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(top = 17.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (back == null) TopAction(Icons.Default.Person, "Blue")
            TopAction(Icons.Default.Menu, "Menú")
        }
    }
}

@Composable
private fun TopAction(icon: ImageVector, label: String, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Surface(shape = CircleShape, color = FinanceCard, modifier = Modifier.size(48.dp)) {
            Icon(icon, contentDescription = label, tint = FinanceBrightBlue, modifier = Modifier.padding(12.dp))
        }
        Text(label, color = FinanceBrightBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SegmentedTabs(selected: Int, onSelected: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(10.dp), color = FinanceCard) {
        Row {
            listOf("Destacados", "Tus productos").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected == index) FinanceBrightBlue else FinanceCard)
                        .clickable { onSelected(index) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                ) {
                    Text(
                        label,
                        color = if (selected == index) Color.White else FinanceBrightBlue,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialHealthCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceCard),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = FinanceBlue)
                Spacer(Modifier.width(10.dp))
                Text("Mejora tu salud financiera", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Close, "Cerrar sugerencia", tint = FinanceBrightBlue)
            }
            Text(
                "Tu saldo previsto a fin de mes es de \$7,221.73. ¿Y si apartas algo?",
                color = FinanceInk,
                lineHeight = 24.sp,
            )
            Text("Saber más", color = FinanceBrightBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(title: String, editable: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = FinanceInk,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
        )
        Spacer(Modifier.weight(1f))
        if (editable) Icon(Icons.Default.Edit, "Editar sección", tint = FinanceBrightBlue)
        else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = FinanceBrightBlue)
    }
}

@Composable
private fun AccountProductCard(onOpenCard: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenCard),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceCard),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row {
                Column {
                    Text("Cuenta", color = FinanceBrightBlue, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("•7390", color = FinanceInk)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("\$7,316.01", color = FinanceInk, fontSize = 28.sp)
                    Text("Saldo disponible", color = FinanceInk)
                }
            }
            ActionStrip("Ver últimos movimientos")
            ActionStrip("Ver ingresos y gastos")
        }
    }
}

@Composable
private fun ActionStrip(text: String) {
    Surface(color = FinanceMuted, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = FinanceBrightBlue, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun QuickActionsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Lo que más haces", editable = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            CircleAction("↔", "Transferir y Dimo")
            CircleAction("▣", "Pagar tarjeta")
            CircleAction("•••", "Ver más")
        }
    }
}

@Composable
private fun CircleAction(symbol: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(105.dp)) {
        Surface(shape = CircleShape, color = FinanceCard, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(symbol, color = FinanceBrightBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = FinanceBrightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CoachCard() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Coach Financiero")
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceCard)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("⌁", color = FinanceBlue, fontSize = 36.sp)
                Text(
                    "Que las emergencias no te tomen sin un colchoncito",
                    color = FinanceInk,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row {
                    Text("Tienes de colchón", fontWeight = FontWeight.Bold, color = FinanceInk)
                    Spacer(Modifier.weight(1f))
                    Text("\$1,216.78", color = FinanceInk, fontSize = 20.sp)
                }
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(FinanceMuted)) {
                    Box(Modifier.fillMaxWidth(.08f).height(8.dp).background(Color(0xFFFFE55B)))
                }
                Text("La cantidad ideal para ti es  \$332,654.58", color = FinanceInk)
                ActionStrip("Ir a 'Coach financiero'")
            }
        }
    }
}

@Composable
private fun InterestCards(onOpen: () -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InterestCard("▰", "Quedan 155 días", "Su primera cuenta,\nsupervisada por ti", onOpen)
        InterestCard("♥", "Quedan 155 días", "Estudiar mejora\nsu futuro", onOpen)
    }
}

@Composable
private fun InterestCard(symbol: String, overline: String, title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(270.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceBrightBlue),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(symbol, color = FinanceSky, fontSize = 44.sp)
            Text(overline, color = Color.White, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Conoce una oportunidad hecha para ti.", color = Color.White, lineHeight = 22.sp)
            Text("¡Me interesa!", color = FinanceSky, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProductsList(onOpenCard: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text("Cuentas", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        CompactProduct("Cuenta", "•7390", "\$7,316.01\nSaldo disponible")
        ActionStrip("Ver ingresos y gastos")
        Text("Tarjetas", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        CompactProduct("Cuenta", "•5732", "")
        Box(Modifier.clickable(onClick = onOpenCard)) {
            CompactProduct("TC3.06", "•5264", "\$118,564.56\nUtilizado")
        }
        Text("Puntos y promociones", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        ActionStrip("◯  Te premiamos con puntos, MSI y promociones especiales.")
    }
}

@Composable
private fun CompactProduct(title: String, suffix: String, amount: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceCard)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, color = FinanceBrightBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(suffix, color = FinanceInk)
            }
            Spacer(Modifier.weight(1f))
            Text(amount, color = FinanceInk, textAlign = TextAlign.End, fontSize = 18.sp)
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selected: Int,
    modifier: Modifier = Modifier,
    onHome: () -> Unit,
    onOpportunities: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp),
        shape = RoundedCornerShape(18.dp),
        color = FinanceCard,
        shadowElevation = 10.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomItem(Icons.Default.Home, "Inicio", selected == 0, onHome)
            BottomItem(Icons.Default.Favorite, "Salud", selected == 1, {})
            BottomItem(Icons.Default.Add, "Oportunidades", selected == 2, onOpportunities)
            BottomItem(Icons.Default.MailOutline, "Notificaciones", selected == 3, {})
            BottomItem(Icons.Default.Person, "Ayuda", selected == 4, {})
        }
    }
}

@Composable
private fun BottomItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(66.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Surface(shape = CircleShape, color = if (selected) FinanceBrightBlue else Color.Transparent) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else FinanceBrightBlue,
                modifier = Modifier.padding(8.dp).size(22.dp),
            )
        }
        Text(label, color = FinanceBrightBlue, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OpportunitiesScreen(onHome: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { PrototypeTopBar("Oportunidades") }
            item { HeroOpportunity() }
            val items = listOf(
                Triple("♥", "ESTUDIAR MEJORARÁ SU FUTURO", "Dona y apoya la educación de jóvenes con recursos limitados."),
                Triple("☂", "SEGURO DE VIAJE", "Asegúrate y protege tu viaje ante un imprevisto."),
                Triple("⚑", "META SEGURA", "Además de ahorrar, protege a tus seres queridos."),
                Triple("▣", "TU DINERO SEGURO CRECE", "Comienza a invertir desde una cantidad accesible."),
                Triple("↗", "INVIERTE SIN COMPLICARTE", "Construye tu futuro financiero paso a paso."),
            )
            items(items.size) { index ->
                OpportunityRow(items[index].first, items[index].second, items[index].third)
            }
        }
        BottomNavigationBar(
            selected = 2,
            modifier = Modifier.align(Alignment.BottomCenter),
            onHome = onHome,
            onOpportunities = {},
        )
    }
}

@Composable
private fun HeroOpportunity() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceBrightBlue)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("▰", color = FinanceSky, fontSize = 72.sp)
            Text("Hasta el 31 diciembre 2026", color = Color.White)
            Text(
                "Su primera cuenta,\nsupervisada por ti",
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 32.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "Apertura sin costo para que acompañes a tu familia en sus primeros pasos financieros.",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FinanceSky, contentColor = FinanceBrightBlue),
                shape = RoundedCornerShape(9.dp),
            ) { Text("Me interesa", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun OpportunityRow(symbol: String, title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceCard)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, color = FinanceSky, fontSize = 42.sp, modifier = Modifier.width(72.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = FinanceInk, fontWeight = FontWeight.Bold)
                Text(body, color = FinanceInk, lineHeight = 22.sp)
                Text("Me interesa", color = FinanceBrightBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CardDetailScreen(onBack: () -> Unit, onMovements: () -> Unit) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { PrototypeTopBar("Tarjeta •45264", back = onBack) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("◯  Apagar tarjeta", color = FinanceBrightBlue, fontWeight = FontWeight.Bold)
                Text("▣  Compra con\ntarjeta digital", color = FinanceBrightBlue, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
        }
        item { CreditCardVisual() }
        item { CreditSummary() }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CircleAction("G", "Agregar a wallet")
                CircleAction("▣", "Pagar tarjeta")
                CircleAction("▤", "Estado de cuenta")
            }
        }
        item { PaymentDetail(onMovements) }
        item { ActionStrip("Tarjetas adicionales                                      ＋") }
        item {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceBlue)) {
                Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("▦", color = FinanceSky, fontSize = 52.sp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("¡Difiere tus compras!", color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 25.sp)
                        Text("Págalas en el tiempo que elijas y quítale presión a tu tarjeta.", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun CreditCardVisual() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF064C8F)),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Row {
                Text("NRX", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("VISA", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Text("••••  ••••  ••••  5264", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CreditSummary() {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceCard)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("TC3.06", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 25.sp)
            Row {
                Text("\$118,564.56\nSaldo utilizado", color = FinanceInk, fontSize = 18.sp)
                Spacer(Modifier.weight(1f))
                Text("\$124,600.00", color = FinanceInk)
            }
            Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(FinanceMuted)) {
                Box(Modifier.fillMaxWidth(.94f).height(12.dp).background(FinanceSky))
            }
            Row {
                Text("Crédito disponible", color = FinanceInk)
                Spacer(Modifier.weight(1f))
                Text("\$6,035.44", color = FinanceInk)
            }
        }
    }
}

@Composable
private fun PaymentDetail(onMovements: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceCard)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Tu próximo pago", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("DETALLE DE PAGO", color = FinanceInk, fontWeight = FontWeight.Bold)
            DetailRow("Fecha límite de pago", "11 de agosto")
            DetailRow("Fecha de corte", "22 de julio")
            HorizontalDivider()
            DetailRow("Pago para no generar intereses", "\$110,609.00")
            DetailRow("Pago mínimo", "\$7,189.58")
            Text(
                "Ver últimos movimientos",
                color = FinanceBrightBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onMovements).padding(vertical = 8.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = FinanceInk)
        Spacer(Modifier.weight(1f))
        Text(value, color = FinanceInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MovementsScreen(onBack: () -> Unit) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { PrototypeTopBar("Tarjeta •45264", back = onBack) }
        item {
            Text("MOVIMIENTOS", color = FinanceInk, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
        item { SegmentedTabs(0, {}) }
        item { Text("28 de julio", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        item { MovementRow("Seguro de vida", "Pago con tarjeta", "\$348.75", "05:03 h") }
        item { Text("22 de julio", color = FinanceInk, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        val movements = listOf(
            "Compra en supermercado" to "\$128.00",
            "Compra en línea" to "\$537.00",
            "Pago con tarjeta" to "\$80.00",
            "Mercado digital" to "\$451.09",
        )
        items(movements.size) { index ->
            MovementRow(movements[index].first, "Movimiento NRX", movements[index].second, "09:10 h")
        }
        item { ActionStrip("Ver todos") }
    }
}

@Composable
private fun MovementRow(title: String, subtitle: String, amount: String, time: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = FinanceCard)) {
        Row(Modifier.fillMaxWidth().padding(18.dp)) {
            Column {
                Text(title, color = FinanceBrightBlue, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = FinanceInk)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = FinanceInk, fontSize = 20.sp)
                Text(time, color = FinanceInk, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun FinancePrototypePreview() {
    MaterialTheme {
        FinancePrototypeApp()
    }
}
