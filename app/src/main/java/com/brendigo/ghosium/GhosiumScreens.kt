package com.brendigo.ghosium

import android.app.Activity
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Native, adaptive and fully interactive Ghosium screens. */
class GhosiumScreens(private val host:Activity) {
    private val white=GhosiumColors.WHITE
    private val muted=GhosiumColors.MUTED
    private val mint=GhosiumColors.MINT
    private val violet=GhosiumColors.VIOLET
    private fun dp(i:Int)=(i*host.resources.displayMetrics.density+.5f).toInt()
    data class TabRow(val title:String,val url:String,val active:Boolean)
    data class DownloadRow(val name:String,val status:String)
    private fun lp(bottom:Int=12)=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(bottom)}
    fun card(color:Int=GhosiumColors.SURFACE,highlight:Boolean=false)=
        GradientDrawable().apply{
            setColor(color);cornerRadius=dp(17).toFloat()
            setStroke(dp(1),if(highlight)mint else GhosiumColors.OUTLINE)
        }
    fun title(t:String,size:Float=16f,color:Int=white,bold:Boolean=false)=TextView(host).apply {
        text=t;textSize=size;setTextColor(color);gravity=Gravity.CENTER_VERTICAL
        if(bold)setTypeface(typeface,Typeface.BOLD)
    }
    fun button(t:String,action:()->Unit)=title(t,17f,white,true).apply{
        background=GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xFF056B70.toInt(),0xFF293777.toInt(),0xFF613B96.toInt())).apply{
            cornerRadius=dp(28).toFloat();setStroke(dp(1),mint)
        }
        gravity=Gravity.CENTER;setPadding(dp(14),dp(15),dp(14),dp(15))
        minHeight=dp(55);contentDescription=t;setOnClickListener{action()}
    }
    fun input(hintText:String,submit:(String)->Unit)=EditText(host).apply{
        setSingleLine(true);textSize=16f;setTextColor(white);setHintTextColor(muted)
        hint=hintText;background=card(0xF2102034.toInt(),true)
        setPadding(dp(17),dp(14),dp(17),dp(14))
        imeOptions=EditorInfo.IME_ACTION_SEARCH
        setOnEditorActionListener{_,id,_->
            if(id==EditorInfo.IME_ACTION_SEARCH || id==EditorInfo.IME_ACTION_GO){
                submit(text.toString());true
            }else false
        }
    }
    private fun column()=LinearLayout(host).apply{
        orientation=LinearLayout.VERTICAL
        setPadding(dp(18),dp(14),dp(18),dp(27))
    }
    fun screen(page:String?=null,branding:Boolean=true,fill:(LinearLayout)->Unit):View{
        val frame=FrameLayout(host).apply{background=AuroraDrawable()}
        val scroll=ScrollView(host).apply{isFillViewport=true;clipToPadding=false}
        val body=column()
        if(branding){
            val hdr=LinearLayout(host).apply{
                orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                setPadding(0,dp(6),0,dp(20))
            }
            hdr.addView(GhosiumMark(host),LinearLayout.LayoutParams(dp(115),dp(115)))
            hdr.addView(title("G H O S I U M",25f,white,true).apply{gravity=Gravity.CENTER})
            hdr.addView(title("Internet po tvojim pravilima",13f,muted).apply{gravity=Gravity.CENTER})
            body.addView(hdr,lp(8))
        }
        if(page!=null)body.addView(title(page,30f,white,true),lp(17))
        fill(body)
        scroll.addView(body);frame.addView(scroll,FrameLayout.LayoutParams(-1,-1))
        return frame
    }
    fun section(c:LinearLayout,t:String){
        c.addView(title(t,19f,white,true).apply{setPadding(0,dp(12),0,dp(9))},lp(3))
    }
    fun row(t:String,subtitle:String,mark:String,action:()->Unit):View{
        val bar=LinearLayout(host).apply{
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
            setPadding(dp(12),dp(10),dp(10),dp(10))
            background=card();minimumHeight=dp(70)
            contentDescription=t;setOnClickListener{action()}
        }
        bar.addView(title(mark,23f,mint,true).apply{gravity=Gravity.CENTER},
            LinearLayout.LayoutParams(dp(41),dp(44)))
        val details=LinearLayout(host).apply{
            orientation=LinearLayout.VERTICAL;setPadding(dp(9),0,0,0)
        }
        details.addView(title(t,16f,white,true).apply{maxLines=2})
        if(subtitle.isNotEmpty())details.addView(title(subtitle,12f,muted).apply{maxLines=2})
        bar.addView(details,LinearLayout.LayoutParams(0,-2,1f))
        bar.addView(title("›",24f,muted).apply{gravity=Gravity.CENTER},
            LinearLayout.LayoutParams(dp(24),dp(38)))
        return bar
    }
    fun home(onQuery:(String)->Unit,onHome:()->Unit,onMarks:()->Unit,onDownloads:()->Unit,
             onSettings:()->Unit,onHistory:()->Unit,onTabs:()->Unit):View=screen { c ->
        val search=input("⌕  Ghosium Search ili URL",onQuery)
        c.addView(search,lp(9))
        c.addView(title("Rezultate pretraživanja pruža DuckDuckGo",12f,muted).apply{
            gravity=Gravity.CENTER
        },lp(18))
        section(c,"Brze poveznice")
        val first=LinearLayout(host);val second=LinearLayout(host)
        fun addTile(parent:LinearLayout,t:String,symbol:String,action:()->Unit,firstColumn:Boolean){
            parent.addView(row(t,"",symbol,action),LinearLayout.LayoutParams(0,dp(85),1f).apply{
                if(firstColumn)rightMargin=dp(5) else leftMargin=dp(5)
            })
        }
        addTile(first,"Početna","⌂",onHome,true)
        addTile(first,"Oznake","♧",onMarks,false)
        addTile(second,"Preuzimanja","↓",onDownloads,true)
        addTile(second,"Privatnost","◇",onSettings,false)
        c.addView(first,lp(11));c.addView(second,lp(15))
        section(c,"Preglednik")
        c.addView(row("Povijest","Nedavno posjećene stranice","◷",onHistory),lp())
        c.addView(row("Kartice","Otvorene web-stranice","▣",onTabs),lp())
    }
    fun welcome(onContinue:()->Unit):View=screen { c ->
        c.addView(title("Dobro došli u Ghosium",26f,white,true).apply{
            gravity=Gravity.CENTER
        },lp(11))
        c.addView(title("Nativni Android preglednik s Ghosium dizajnom i kontrolama privatnosti.",14f,muted).apply{
            gravity=Gravity.CENTER
        },lp(25))
        c.addView(row("Brzo pregledavanje","Android System WebView","ϟ",{ featureInfo("Brzo pregledavanje","Ghosium prikazuje web-stranice pomoću Android System WebView tehnologije.") }),lp())
        c.addView(row("Privatnost","Kontrole kolačića i JavaScripta","◇",{ featureInfo("Privatnost","Omogućeno je upravljanje kolačićima i JavaScriptom. Anonimno pregledavanje nije uključeno.") }),lp())
        c.addView(row("Preuzimanja","Android upravitelj preuzimanja","↓",{ featureInfo("Preuzimanja","Preuzete datoteke spremljene su Android sustavskim upraviteljem preuzimanja.") }),lp(27))
        c.addView(button("Započni  →",onContinue),lp())
    }
    fun tabs(items:List<TabRow>,onOpen:(Int)->Unit,onClose:(Int)->Unit,onNew:()->Unit):View=
        screen("Kartice"){c->
            items.forEachIndexed{index,item->
                val bar=LinearLayout(host).apply{
                    gravity=Gravity.CENTER_VERTICAL
                    setPadding(dp(13),dp(12),dp(10),dp(12))
                    background=card(if(item.active)0xF21F3249.toInt() else GhosiumColors.SURFACE,
                        item.active)
                    minimumHeight=dp(86)
                }
                val detail=LinearLayout(host).apply{orientation=LinearLayout.VERTICAL}
                detail.addView(title(item.title,16f,white,true).apply{maxLines=2})
                detail.addView(title(item.url.ifBlank{"Ghosium • Nova kartica"},12f,muted).apply{maxLines=2})
                if(item.active)detail.addView(title("● Aktivna kartica",12f,mint))
                detail.setOnClickListener{onOpen(index)}
                bar.addView(detail,LinearLayout.LayoutParams(0,-2,1f))
                bar.addView(title("×",27f,violet).apply{
                    gravity=Gravity.CENTER;contentDescription="Zatvori karticu "+(index+1)
                    setOnClickListener{onClose(index)}
                },LinearLayout.LayoutParams(dp(45),dp(46)))
                c.addView(bar,lp(10))
            }
            c.addView(button("＋  Nova kartica",onNew),lp())
        }
    fun collection(page:String,items:List<BrowserStore.Item>,onOpen:(String)->Unit,
                   onDelete:((BrowserStore.Item)->Unit)?=null):View=screen(page){c->
        val results=LinearLayout(host).apply{orientation=LinearLayout.VERTICAL}
        fun populate(q:String){
            results.removeAllViews()
            val matches=items.filter{it.title.contains(q,true)||it.url.contains(q,true)}
            if(matches.isEmpty())results.addView(title("Nema spremljenih stavki.",15f,muted),lp())
            matches.forEach{item->
                val box=LinearLayout(host).apply{
                    gravity=Gravity.CENTER_VERTICAL;setPadding(dp(13),dp(12),dp(7),dp(12))
                    background=card();minimumHeight=dp(67)
                }
                val inner=LinearLayout(host).apply{orientation=LinearLayout.VERTICAL}
                inner.addView(title(item.title.ifBlank{item.url},16f,white,true).apply{maxLines=2})
                inner.addView(title(item.url,12f,muted).apply{maxLines=2})
                inner.setOnClickListener{onOpen(item.url)}
                box.addView(inner,LinearLayout.LayoutParams(0,-2,1f))
                if(onDelete!=null)box.addView(title("×",25f,violet).apply{
                    gravity=Gravity.CENTER;contentDescription="Ukloni "+item.title
                    setOnClickListener{onDelete(item)}
                },LinearLayout.LayoutParams(dp(44),dp(46)))
                results.addView(box,lp(9))
            }
        }
        val search=input("⌕  Pretraži "+page.lowercase()) {populate(it)}
        search.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,st:Int,count:Int,after:Int){}
            override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){populate(s.toString())}
            override fun afterTextChanged(s:Editable?){}
        })
        c.addView(search,lp(16));populate("");c.addView(results)
    }
    fun search(history:List<BrowserStore.Item>,initial:String,onSearch:(String)->Unit):View=
        screen("Ghosium Search"){c->
            val suggestions=LinearLayout(host).apply{orientation=LinearLayout.VERTICAL}
            fun populate(q:String){
                suggestions.removeAllViews()
                history.filter{it.url.contains(q,true)||it.title.contains(q,true)}.take(8).forEach{
                    item->suggestions.addView(row(item.title,item.url,"◷"){onSearch(item.url)},lp(10))
                }
            }
            val input=input("Ghosium Search ili URL",onSearch)
            input.setText(initial);input.setSelection(input.text.length)
            input.addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s:CharSequence?,st:Int,count:Int,after:Int){}
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){populate(s.toString())}
                override fun afterTextChanged(s:Editable?){}
            })
            c.addView(input,lp(8))
            c.addView(title("Rezultate pretraživanja pruža DuckDuckGo",12f,muted).apply{
                gravity=Gravity.CENTER
            },lp(16))
            c.addView(button("Pretraži  →"){onSearch(input.text.toString())},lp(13))
            section(c,"Nedavno pregledavano")
            populate(initial);c.addView(suggestions)
        }
    fun downloads(items:List<DownloadRow>,openManager:()->Unit):View=screen("Preuzimanja"){c->
        c.addView(title("Preuzimanjima upravlja sustav Android.",14f,muted),lp(14))
        if(items.isEmpty())c.addView(title("Još nema preuzimanja.",16f,muted),lp())
        items.forEach{item->c.addView(row(item.name,item.status,"↓",openManager),lp(9))}
        c.addView(button("Otvori Android preuzimanja  ↗",openManager),lp())
    }
    fun settings(js:Boolean,cookies:Boolean,onJs:(Boolean)->Unit,onCookies:(Boolean)->Unit,
                 onClear:()->Unit,onInfo:()->Unit):View=screen("Postavke i privatnost"){c->
        c.addView(row("Bez računa","Lokalno korištenje bez Ghosium prijave","◇",onInfo),lp(16))
        section(c,"Općenito")
        fun switchRow(name:String,details:String,selected:Boolean,change:(Boolean)->Unit){
            val bar=LinearLayout(host).apply{
                gravity=Gravity.CENTER_VERTICAL;setPadding(dp(14),dp(13),dp(14),dp(13))
                background=card();minimumHeight=dp(73)
            }
            val left=LinearLayout(host).apply{orientation=LinearLayout.VERTICAL}
            left.addView(title(name,16f,white,true))
            left.addView(title(details,12f,muted).apply{maxLines=2})
            bar.addView(left,LinearLayout.LayoutParams(0,-2,1f))
            bar.addView(Switch(host).apply{
                isChecked=selected;contentDescription=name
                thumbTintList=ColorStateList.valueOf(if(selected)mint else white)
                setOnCheckedChangeListener{_,v->change(v)}
            })
            c.addView(bar,lp(10))
        }
        c.addView(row("Ghosium Search","Rezultate pruža DuckDuckGo","⌕",onInfo),lp())
        switchRow("JavaScript","Izvršavanje JavaScripta na stranicama",js,onJs)
        switchRow("Kolačići","Dopusti kolačiće web-stranicama",cookies,onCookies)
        c.addView(row("Obriši podatke","Povijest, kolačići i predmemorija","⌫",onClear),lp(12))
        section(c,"Sigurnost")
        c.addView(row("Informacije o zaštiti","HTTPS, Safe Browsing i ograničenja","◇",onInfo),lp())
    }
    private fun featureInfo(heading:String, description:String) {
        AlertDialog.Builder(host).setTitle(heading).setMessage(description)
            .setPositiveButton("U redu",null).show()
    }
    fun information():View=screen("Sigurnost Ghosiuma"){c->
        c.addView(title("Ghosium koristi Android System WebView i zaštićene HTTPS veze. Nevaljani SSL certifikati se odbijaju. Safe Browsing štiti od poznatih opasnih web-stranica.",16f,white),lp(20))
        c.addView(title("Pretraživanje otvara rezultate na DuckDuckGo web-stranici. Ghosium trenutačno nema potpuno odvojeni privatni profil ili ugrađeni blokator svih tragača. Preglednik nije anonimizacijski alat.",15f,muted),lp())
    }
}
