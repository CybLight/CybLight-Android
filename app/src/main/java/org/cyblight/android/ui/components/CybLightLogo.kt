package org.cyblight.android.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.cyblight.android.R

@Composable
fun CybLightLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Image(
        painter = painterResource(R.drawable.ic_cyblight_logo),
        contentDescription = stringResource(R.string.logo_content_desc),
        modifier = modifier.size(size),
    )
}
