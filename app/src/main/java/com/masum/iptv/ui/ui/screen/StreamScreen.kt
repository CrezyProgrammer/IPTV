package com.masum.iptv.ui.ui.screen

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.masum.iptv.R
import com.masum.iptv.ui.VideoPlayerActivity
import com.masum.iptv.ui.home.ui.theme.IPTVTheme
import com.masum.iptv.utils.*

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
 fun StreamScreen() {

val context = LocalContext.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    androidx.compose.material.Text(
                        "Simple TopAppBar",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    androidx.compose.material.IconButton(onClick = { /* doSomething() */ }) {
                        androidx.compose.material.Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Localized description"
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Play") },
                icon = { Icon(Icons.Filled.PlayArrow, "") },
                onClick = {
                    Log.i("123321", "main: url=${url.value} \n cookie=${cookie.value}\n origin=${origin.value}\n referer=${refer.value}\n" +
                            "drm=${drm.value}\n userAgent=${userAgent.value}\n scheme=${scheme.value}")
                    /*   findNavController().navigate(
                           DashboardFragmentDirections.actionNavigationDashboardToFullScreenPlayerFragment(
                               url.value,cookie.value,origin.value,refer.value,drm.value,userAgent.value,scheme.value))*/
                    val videoIntent=(Intent( context, VideoPlayerActivity::class.java))

                    videoIntent.putExtra(LINK,url.value)
                    if(cookie.value.isNotEmpty()) videoIntent.putExtra(COOKIE,cookie.value)
                    if(origin.value.isNotEmpty()) videoIntent.putExtra(ORIGIN,origin.value)
                    if(refer.value.isNotEmpty()) videoIntent.putExtra(REFERER,refer.value)
                    if(drm.value.isNotEmpty()) videoIntent.putExtra(DRM,drm.value)
                    if(userAgent.value.isNotEmpty()&&userAgents.contains(userAgent.value)) videoIntent.putExtra(
                        USER_AGENT,userAgent.value)
                    videoIntent.putExtra(SCHEME,scheme.value)

                     context.startActivity(videoIntent)



                },
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .fillMaxHeight(0.075f),
                shape = AbsoluteRoundedCornerShape(50),
            )
        },

        floatingActionButtonPosition = FabPosition.End,
        content = { innerPadding->
            Box(modifier = Modifier.padding(innerPadding))
            { editText() }
        }
    )
}

lateinit var url: MutableState<String>
lateinit var origin: MutableState<String>
lateinit var cookie: MutableState<String>
lateinit var refer: MutableState<String>
lateinit var drm: MutableState<String>
lateinit var userAgent:MutableState<String>
lateinit var scheme:MutableState<String>
val userAgents= arrayListOf("" +
        "Chrome/{Chrome Rev} Mobile Safari/{WebKit Rev}",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36",
    "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko",
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:105.0) Gecko/20100101 Firefox/105.0",
    "Mozilla/5.0 (iPhone; CPU iPhone OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148",
    "Mozilla/5.0 (Linux; U; Android 4.2; ru-ru; Nokia_X Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.2 Mobile Safari/E7FBAF",
    ""
)

val URL="url"
var url_value=""
@Composable
fun editText() {
    val context = LocalContext.current

    IPTVTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        )
        {
            val preference=context.getSharedPreferences("configuration", Context.MODE_PRIVATE)

            url = remember { mutableStateOf(url_value) }
            refer = remember { mutableStateOf("") }
            origin = remember { mutableStateOf("") }
             drm = remember { mutableStateOf("") }
            cookie = remember { mutableStateOf("") }
            userAgent = remember { mutableStateOf(preference.getString(USER_AGENT_VALUE,null)?:"") }
            scheme = remember { mutableStateOf(preference.getString(SCHEME,null)?:"") }

            Column(
                modifier = Modifier.padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                textBox(url, "Media Stream Url")
                textBox(cookie, "Cookie Value")
                textBox(origin, "Origin Value")
                textBox(refer, "Refer Value")
                textBox(drm, "DRM License URL")

                val userAgentSuggestions = listOf("Default", "Chrome(Android)", "Chrome(PC)","IE(PC)", "Firefox(PC)","IPhone","Nokia","Custom")
                val drmSchemeSuggestions = listOf("widevine", "playready", "clearkey")
                val userAgentSelectedText : MutableState<String> = remember { mutableStateOf(preference.getString(
                    USER_AGENT,null)?:userAgentSuggestions[0]) }
                val drmSchemeSelectedText : MutableState<String> = remember { mutableStateOf(preference.getString(
                    SCHEME,null)?:drmSchemeSuggestions[0]) }

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)){

                        DropdownMenu("UserAgent",userAgentSelectedText.value, userAgentSuggestions,true){

                            userAgent.value=it.text
                        }
                    }
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(5.dp)){

                        DropdownMenu("DrmScheme", drmSchemeSelectedText.value, drmSchemeSuggestions,false)
                        {
                            scheme.value=it.text
                        }
                    }
                }

            }
        }

    }
}
lateinit var drmSchemeSelectedText : MutableState<String>


@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DropdownMenu(
    label: String,
    selectedText:String ,
    suggestions: List<String>,
    isUserAgent: Boolean,
    onValueChange: (TextFieldValue) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf(selectedText) }
    val openDialog = remember { mutableStateOf(false) }


    var textfieldSize by remember { mutableStateOf(Size.Zero) }

    val icon = if (expanded)
        Icons.Filled.KeyboardArrowUp //it requires androidx.compose.material:material-icons-extended
    else
        Icons.Filled.ArrowDropDown

    val font = FontFamily(
        Font(R.font.akaya_telivigala, FontWeight.Normal)
    )
    val preference= LocalContext.current.getSharedPreferences("configuration", Context.MODE_PRIVATE).edit()


    OutlinedTextField(
        readOnly = true,
        singleLine = true,
        value = TextFieldValue(title),
        onValueChange = onValueChange,
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            expanded=!expanded
                        }
                    }
                }
            },
        modifier = Modifier
            .clickable { expanded = !expanded }
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                //This value is used to assign to the DropDown the same width
                textfieldSize = coordinates.size.toSize()
            },

        label = { Text(label, fontFamily = font) },
        trailingIcon = {
            Icon(icon, "contentDescription",
                Modifier.clickable {
                    expanded = !expanded
                })
        }
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .width(with(LocalDensity.current) { textfieldSize.width.toDp() })
    ) {
        suggestions.forEachIndexed { position, label ->

            DropdownMenuItem(
                { Text(text = label) },
                onClick = {
                    expanded = !expanded
                    title = label
                    if(isUserAgent){
                        preference.putString(USER_AGENT,label).apply()
                    }
                    else{preference.putString(SCHEME,label).apply()}

                    if (position == 7)
                        openDialog.value = true

                    else {
                        userAgent.value = userAgents[position]
                        preference.putString(USER_AGENT_VALUE, userAgents[position]).apply()
                    }
                })
        }


    }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {
                openDialog.value = false
            },
            title = {
                Text(text = "Title")
            },


            text = {
                Column() {
                    TextField(
                        value = userAgent.value,
                        onValueChange = { userAgent.value = it
                            preference.putString(USER_AGENT_VALUE, it).apply()

                        }
                    )
                }
            },
            dismissButton ={
                TextButton(onClick = { openDialog.value = false})
            { Text(text = "Dismiss") }


            },

            confirmButton  = {

                TextButton(onClick = { openDialog.value = false})
                { Text(text = "OK") }

            })
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun textBox(text: MutableState<String>, hint: String) {
    val font= FontFamily(
        Font(R.font.akaya_telivigala, FontWeight.Normal)
    )

    val trailingIconView = @Composable {
        IconButton(
            onClick = {
                text.value = ""
            },
        ) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "",
                tint = Color.Black
            )
        }
    }

    androidx.compose.material3.OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(0.dp, 0.dp, 0.dp, 5.dp),

        value = text.value, onValueChange = {text.value=it},
        label ={ Text(text=hint,fontFamily=font) },
        trailingIcon = if (text.value.isNotBlank()) trailingIconView else null,

        )

}