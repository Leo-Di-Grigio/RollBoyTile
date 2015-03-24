package game.cycle.scene.ui.widgets.windows;

import java.util.ArrayList;
import java.util.HashMap;

import game.cycle.scene.game.world.creature.Creature;
import game.cycle.scene.game.world.database.Database;
import game.cycle.scene.game.world.dialog.DialogBlock;
import game.cycle.scene.game.world.dialog.DialogProto;
import game.cycle.scene.ui.Scroll;
import game.cycle.scene.ui.UI;
import game.cycle.scene.ui.widgets.Button;
import game.cycle.scene.ui.widgets.List;
import game.cycle.scene.ui.widgets.ListItem;
import game.cycle.scene.ui.widgets.Window;
import game.resources.Resources;
import game.resources.Tex;
import game.script.ui.game.ui_Talk;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class WindowDialog extends Window implements Scroll {

	private Creature npc;
	
	public static final String uiList = "dialog-topic-list";
	public static final String uiTalk = "dialog-topic-talk";
	public List list;
	public Button talk;
	
	private ArrayList<String> dialog;
	
	public WindowDialog(String title, UI ui, int layer) {
		super(title, ui, Alignment.CENTERLEFT, 450, 600, 140, 0, layer);
		this.setTexNormal(Resources.getTex(Tex.uiBackgroundNormal));
		this.scroll = true;
		loadWidgets();
	}
	
	private void loadWidgets() {
		this.closeButton(true);
		
		list = new List(uiList);
		list.setSize(260, 450);
		list.setVisible(25);
		list.setPosition(Alignment.UPRIGTH, 260, -130);
		this.add(list);
		
		talk = new Button(uiTalk, "Say");
		talk.setSize(128, 32);
		talk.setPosition(Alignment.UPRIGTH, 130, -96);
		talk.setScript(new ui_Talk(this));
		this.add(talk);
	}

	public void setCreature(Creature character) {
		dialog = new ArrayList<String>();
		list.clear();
		this.npc = character;
		
		HashMap<Integer, DialogProto> dialog = Database.getBaseDialog();
		
		ArrayList<Boolean> mask = new ArrayList<Boolean>();
		mask.add(0, true);
		
		for(int key: character.proto.dialogTopics){
			DialogProto proto = dialog.get(key);
			
			ArrayList<String> data = new ArrayList<String>();
			data.add(0, ""+key);
			data.add(1, proto.title);
			
			ListItem item = new ListItem(data, mask);
			item.setFormatter("");
			list.addElement(item);
		}
		
		DialogBlock block = new DialogBlock(Database.getDialog(character.proto.dialogStart), true);
		addBlock(block);
	}
	
	public void addBlock(DialogBlock block) {
		int size = block.beginText.length + block.endText.length;
		if(dialog.size() + size > linesmax){
			this.scrollAmount += size;
		}
	
		for(String line: block.beginText){
			dialog.add(line);
		}
		
		for(String line: block.endText){
			dialog.add(line);
		}
	}

	@Override
	public void draw(SpriteBatch sprites) {
		if(npc != null){
			super.draw(sprites);
			sprites.draw(npc.avatar, x, y + sizeY - 128, 128, 128);
			
			String textNM = npc.proto.name + " GUID: " + npc.getId();
			font.drawWrapped(sprites, textNM, x + 128, y - font.getBounds(textNM).height*2 + sizeY, font.getBounds(textNM).width, BitmapFont.HAlignment.CENTER);
			drawLine(sprites);
		}
	}
	
	private static final int linesmax = 25;
	private void drawLine(SpriteBatch sprites){
		int total = linesTotal();
		for(int i = scrollAmount, j = 0; j < linesmax && i < total; ++i, ++j){
			font.draw(sprites, dialog.get(i), x + 5, y + sizeY - j * lineHeight - 140);
		}
	}
	
	@Override
	public void dispose() {
		super.dispose();
		npc = null;
	}

	// scroll
	protected int scrollAmount = 0;
	protected static final int lineHeight = 18;
	
	@Override
	public void scroll(int amount) {
		scrollAmount += amount;
		
		if(scrollAmount < 0 || scrollAmount >= linesTotal()){
			scrollAmount -= amount;
		}
	}
	
	protected int linesTotal(){
		return dialog.size();
	}
}