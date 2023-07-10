package me.xmrvizzy.skyblocker.skyblock.tabhud.widget;

import me.xmrvizzy.skyblocker.skyblock.tabhud.util.Ico;
import me.xmrvizzy.skyblocker.skyblock.tabhud.widget.component.IcoTextComponent;
import me.xmrvizzy.skyblocker.skyblock.tabhud.widget.component.PlainTextComponent;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

// this widget shows info about ongoing profile/account upgrades
// or not, if there aren't any
// TODO: not very pretty atm

public class UpgradeWidget extends Widget {

    private static final MutableText TITLE = Text.literal("Upgrade Info").formatted(Formatting.GOLD,
            Formatting.BOLD);

    public UpgradeWidget(String footertext) {
        super(TITLE, Formatting.GOLD.getColorValue());
        if (footertext == null) {
            this.addComponent(new PlainTextComponent(Text.literal("No data").formatted(Formatting.GRAY)));
            this.pack();
            return;
        }

        if (!footertext.contains("Upgrades")) {
            this.addComponent(new PlainTextComponent(Text.of("Currently no upgrades...")));
            this.pack();
            return;
        }

        String interesting = footertext.split("Upgrades")[1];
        String[] lines = interesting.split("\n");

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().length() < 3) { // empty line is §s
                break;
            }
            IcoTextComponent itc = new IcoTextComponent(Ico.SIGN, Text.of(lines[i]));
            this.addComponent(itc);
        }
        this.pack();
    }

}
