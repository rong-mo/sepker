package com.sepker.ui;

import arc.Core;
import arc.files.Fi;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import com.sepker.SepkerMod;

public class SepkerStartupUI extends Dialog {

    // 全部设为 public，彻底根除安卓 IllegalAccessError
    public Label contentLabel;
    public TextButton switchButton;
    public int currentTab = 0; // 0 = 杂项, 1 = 更新公告

    public String miscText = "[lightgray]暂无杂项内容。";
    public String changelogText = "[lightgray]暂无更新公告。";

    public SepkerStartupUI() {
        super("黄队的奇妙大冒险");
        
        setFillParent(true);
        setBackground(Styles.black6);
        setModal(true);
        setMovable(false);

        loadTextFiles();
        buildUI();
    }

    public void loadTextFiles() {
        try {
            Fi modRoot = Vars.mods.getMod(SepkerMod.class).root;
            
            Fi miscFile = modRoot.child("misc.txt");
            if (!miscFile.exists()) miscFile = modRoot.child("assets/misc.txt");
            if (miscFile.exists()) {
                miscText = miscFile.readString();
            }

            Fi changelogFile = modRoot.child("changelog.txt");
            if (!changelogFile.exists()) changelogFile = modRoot.child("assets/changelog.txt");
            if (changelogFile.exists()) {
                changelogText = changelogFile.readString();
            }
        } catch (Exception e) {
            // 读取失败保持默认文本，不崩溃
        }
    }

    public void buildUI() {
        cont.clear();

        Table topTable = new Table();
        TextureRegion logo = Core.atlas.find("黄队的奇妙大冒险-sepker-logo");
        if (logo.found()) {
            topTable.add(new Image(logo)).size(128f).left().pad(20f);
        }
        cont.add(topTable).growX().left().row();

        Table textContainer = new Table();
        textContainer.background(Styles.black3);
        
        contentLabel = new Label("");
        // 默认字体，完美支持颜色标签
        contentLabel.setStyle(new Label.LabelStyle(Fonts.def, Color.white));
        contentLabel.setWrap(true);
        contentLabel.setAlignment(Align.topLeft);

        // 字体缩放
        contentLabel.setFontScale(1f); 

        ScrollPane pane = new ScrollPane(contentLabel);
        pane.setFadeScrollBars(false);
        pane.setScrollingDisabled(true, false); 
        pane.setForceScroll(false, true); 

        textContainer.add(pane).grow().pad(20f);
        
        cont.add(textContainer).grow().pad(20f).row();

        Table bottomTable = new Table();

        TextButton closeBtn = new TextButton("关闭");
        closeBtn.clicked(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        });

        switchButton = new TextButton("查看更新公告");
        switchButton.clicked(new Runnable() {
            @Override
            public void run() {
                currentTab = (currentTab == 0) ? 1 : 0;
                updateText();
            }
        });

        TextButton noRemindBtn = new TextButton("不再提醒");
        noRemindBtn.clicked(new Runnable() {
            @Override
            public void run() {
                Core.settings.put("sepker-no-startup-ui", true);
                hide();
            }
        });

        bottomTable.add(closeBtn).size(140f, 60f).pad(5f);
        bottomTable.add(switchButton).size(200f, 60f).pad(5f);
        bottomTable.add(noRemindBtn).size(140f, 60f).pad(5f);
        
        cont.add(bottomTable).padBottom(20f).row();

        updateText();
    }

    public void updateText() {
        if (currentTab == 0) {
            contentLabel.setText(miscText);
            switchButton.setText("查看更新公告");
        } else {
            contentLabel.setText(changelogText);
            switchButton.setText("返回杂项");
        }
    }
}