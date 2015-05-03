package game.cycle.scene.ui.widgets.windows;

import java.util.ArrayList;
import java.util.HashMap;

import resources.Resources;
import resources.tex.Tex;
import tools.Const;
import ui.Alignment;
import ui.Window;
import ui.widgets.ListItem;
import ui.widgets.used.Button;
import ui.widgets.used.List;
import game.SceneGame;
import game.cycle.scene.ui.list.UIGame;
import game.script.ui.editor.ui_EditorMode;
import game.script.ui.editor.ui_UIGameEditor;
import game.state.database.Database;
import game.state.database.proto.GOProto;

public class WindowEditorGO extends Window {
	
	private UIGame uigame;
	
	public static final String uiAdd = "editor-go-add";
	public static final String uiEdit = "editor-go-edit";
	public static final String uiList = "editor-go-list";
	
	public Button add;
	public Button edit;
	public List   list;
	
	public WindowEditorGO(String title, UIGame ui, int layer, SceneGame scene) {
		super(title, ui, Alignment.CENTER, 326, 24, 0, 0, layer);
		this.uigame = ui;
		this.setTexNormal(Resources.getTex(Tex.UI_LIST_LINE));
		this.setText("Game Objects");
		
		loadWidgets(scene);
		loadGOList();
	}

	private void loadWidgets(SceneGame scene) {
		this.closeButton(true);
		this.closeButton.setScript(new ui_UIGameEditor(uigame, UIGame.EDITOR_GO));
		this.lockButton(true);
		
		add = new Button(uiAdd, "Add");
		add.setSize(64, 32);
		add.setPosition(Alignment.UPRIGTH, -262, -24);
		add.setScript(new ui_EditorMode(uigame, UIGame.MODE_GO_ADD));
		this.add(add);
		
		edit = new Button(uiEdit, "Edit");
		edit.setSize(64, 32);
		edit.setPosition(Alignment.UPRIGTH, -262, -58);
		edit.setScript(new ui_EditorMode(uigame, UIGame.MODE_GO_EDIT));
		this.add(edit);
		
		list = new List(uiList);
		list.setSize(260, 300);
		list.setVisible(16);
		list.setPosition(Alignment.UPRIGTH, 0, -24);
		this.add(list);
	}
	
	private void loadGOList() {
		list.clear();
		HashMap<Integer, GOProto> base = Database.getBaseGO();
		
		for(Integer key: base.keySet()){
			ArrayList<String> data = new ArrayList<String>();
			data.add(0, ""+key);
			data.add(1, base.get(key).title());
			
			ListItem item = new ListItem(data);
			list.addElement(item);
		}
	}
	
	public int getSelectedListGO(){
		ListItem item = list.getSelected();
		
		if(item != null){
			return Integer.parseInt(item.get(0));
		}
		else{
			return Const.INVALID_ID;
		}
	}
}