package game.state;

import game.cycle.scene.ui.list.UIGame;
import game.script.game.event.Logic;
import game.state.database.Database;
import game.state.database.proto.LocationProto;
import game.state.event.Event;
import game.state.location.Editor;
import game.state.location.Location;
import game.state.location.creature.Player;
import game.state.location.go.GO;
import game.state.location.manager.LocationManager;
import game.state.skill.Skill;

import java.awt.Point;

import lua.LuaEngine;
import resources.Cursors;
import resources.Resources;
import resources.tex.Tex;
import tools.Const;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Disposable;

import cycle.input.UserInput;

public class State implements Disposable {	
	// data
	private Globals globals;
	private Location currentLocation;
	
	// cursor
	private Point select;
	private Vector3 cursorPos;
	private Sprite tileSelectCursor;
	private Sprite tileWaypoint;
	
	public State(UIGame uimenu) {
		globals = new Globals();
		uimenu.setPlayer(globals.getPlayer());
		
		select = new Point();
		cursorPos = new Vector3();
		tileSelectCursor = new Sprite(Resources.getTex(Tex.GAMEPLAY_SELECT));
		tileWaypoint = new Sprite(Resources.getTex(Tex.GAMEPLAY_WP));
		
		LocationProto proto = Database.getLocation(0);
		
		if(proto == null){
			proto = new LocationProto("Default", "default", "", null);
			Database.insertLocation(proto);
			Database.loadLocations();
			LocationManager.createNew(proto, 32, 32, 1);
		}
		else{
			LocationManager.createNew(proto, 32, 32, 1);
		}
	}

	public void loadLocation(int id, int playerPosX, int playerPosY){
		if(currentLocation != null){
			LuaEngine.executeLocationEvent(new Event(Event.EVENT_LOCATION_CHANGE, null, null));
			currentLocation.dispose();
			currentLocation = null;
		}
		
		currentLocation = LocationManager.loadLocation(id);
		
		// place player
		if(currentLocation != null && currentLocation.inBound(playerPosX, playerPosY)){
			currentLocation.addObject(getPlayer(), playerPosX, playerPosY, false);
		}
		else{
			currentLocation.addObject(getPlayer(), 0, 0, false);
			playerPosX = 0;
			playerPosY = 0;
		}
		
		getPlayer().resetAp();
		
		if(getPlayer().getDraggedObject() != null){
			if(getPlayer().getDraggedObject().isGO()){
				GO go = (GO)getPlayer().getDraggedObject();
				Editor.goAdd(currentLocation, go, playerPosX, playerPosY);
			}
		}
		
		if(currentLocation != null && currentLocation.proto.eventScript() != null){
			LuaEngine.load(currentLocation.proto.eventScript());
		}
		
		currentLocation.requestUpdate();
		LuaEngine.executeLocationEvent(new Event(Event.EVENT_LOCATION_LOAD, null, null));
	}

	public void saveLocation() {
		if(currentLocation != null){
			LocationManager.saveLocation(currentLocation);
		}
	}
	
	public Location getLocation(){
		return currentLocation;
	}
	
	public Globals getGlobals(){
		return globals;
	}
	
	public Player getPlayer() {
		return globals.getPlayer();
	}
	
	// Update
	public void update(OrthographicCamera camera, UIGame ui, boolean losMode) {		
		// pick a cursor position
		Ray ray = camera.getPickRay(UserInput.mouseX, UserInput.mouseY);
    	float distance = -ray.origin.z/ray.direction.z;
    	cursorPos = new Vector3();
    	cursorPos.set(ray.direction).scl(distance).add(ray.origin);
    	
    	// characters update
    	currentLocation.update(getPlayer(), camera, ui, losMode);
	}

	public void updateFreeCamera(OrthographicCamera camera) {
		camera.translate(-camera.position.x, -camera.position.y);
		camera.translate(getPlayer().getSprite().getX() + Const.TILE_SIZE/2, getPlayer().getSprite().getY());
	}
	
	// Draw
	public void draw(SpriteBatch batch, OrthographicCamera camera, UIGame ui, boolean losMode) {
		if(currentLocation != null){
			// draw location
			currentLocation.draw(camera, batch, losMode, ui, getPlayer());
	
			// draw player waypoints
			if(getPlayer().isMoved()){
				if(getPlayer().getPath() != null){
					for(Point point: getPlayer().getPath()){
						tileWaypoint.setPosition((float)(point.getX()*Const.TILE_SIZE), (float)(point.getY()*Const.TILE_SIZE));
						tileWaypoint.draw(batch);	
					}
				}
			}
			
			// update cursor
			updateCursor(batch, ui);
		}
	}

	private void updateCursor(SpriteBatch batch, UIGame ui) {
		select.x = ((int)cursorPos.x) / Const.TILE_SIZE;
		select.y = ((int)cursorPos.y) / Const.TILE_SIZE;
		
		if(ui.isSelected()){
			Cursors.setCursor(Cursors.cursorDefault);
		}
		else{
			if(getPlayer().getUsedSkill() != null){
				if(ui.getSkillMode()){
					Cursors.setCursor(Cursors.cursorCast);
					
					if(currentLocation.inBound(select.x, select.y)){
						batch.draw(tileSelectCursor, select.x*Const.TILE_SIZE, select.y*Const.TILE_SIZE);
					}
				}
				else{	
					setSceneCursor(batch);
				}
			}
			else{
				Cursors.setCursor(Cursors.cursorDefault);	
			}
		}
	}
	
	private void setSceneCursor(SpriteBatch batch) {
		if(currentLocation.inBound(select.x, select.y)){
			int posX = select.x * Const.TILE_SIZE;
			int posY = select.y * Const.TILE_SIZE;
			
			tileSelectCursor.setPosition(posX, posY);
			tileSelectCursor.draw(batch);
		
			if(isInterractive(select.x, select.y, getPlayer().getGUID())){
				Cursors.setCursor(Cursors.cursorTalking);
			}
			else{
				Cursors.setCursor(Cursors.cursorDefault);
			}
		}
		else{
			Cursors.setCursor(Cursors.cursorDefault);
		}
	}

	private boolean isInterractive(int x, int y, int playerid) {
		return currentLocation.isInteractive(x, y, playerid);
	}

	@Override
	public void dispose() {
		currentLocation.dispose();
		tileSelectCursor = null;
	}

	public void moveUp() {
		getPlayer().getSprite().translate(0.0f, 1.0f);
	}

	public void moveDown() {
		getPlayer().getSprite().translate(0.0f, -1.0f);
	}

	public void moveLeft() {
		getPlayer().getSprite().translate(-1.0f, 0.0f);
	}

	public void moveRight() {
		getPlayer().getSprite().translate(1.0f, 0.0f);
	}

	// Turn based mode
	public void requestSwitchMode(boolean playerInit) {
		currentLocation.switchMode(playerInit);
	}

	public void requestTurnMode(boolean playerInit) {
		currentLocation.turnMode(playerInit);
	}
	
	public void requestEndTurn() {
		currentLocation.endTurn();
	}

	// Click event
	public void actionFirst(UIGame ui) {
		switch(ui.getMode()) {
			case UIGame.MODE_GO_ADD:
				Editor.goAdd(currentLocation, ui, select.x, select.y, true);
				break;
				
			case UIGame.MODE_GO_EDIT:
				Editor.goEdit(currentLocation, select.x, select.y, ui);
				break;
					
			case UIGame.MODE_NPC_ADD:
				Editor.npcAdd(currentLocation, ui, select.x, select.y, true);
				break;
					
			case UIGame.MODE_NPC_EDIT:
				Editor.npcEdit(currentLocation, select.x, select.y, ui);
				break;
				
			case UIGame.MODE_TERRAIN_BRUSH_1:
			case UIGame.MODE_TERRAIN_BRUSH_2:
			case UIGame.MODE_TERRAIN_BRUSH_3:
			case UIGame.MODE_TERRAIN_FILL:
				Editor.editorTerrain(currentLocation, select.x, select.y, ui, ui.getMode());
				break;
					
			case UIGame.MODE_SKILL_NULL:
			case UIGame.MODE_SKILL_MELEE:
			case UIGame.MODE_SKILL_RANGE:
			case UIGame.MODE_SKILL_SPELL:
				getPlayer().useSkill(currentLocation, getPlayer().getUsedSkill(), select.x, select.y);
				break;
				
			case Const.INVALID_ID:
			default:
				playerAction(ui);
				break;
		}
	}
	
	private void playerAction(UIGame ui) {
		if(currentLocation.inBound(select.x, select.y)){
			getPlayer().move(currentLocation, select.x, select.y);
			if(getPlayer().isMoved()){
				ui.openContainer(null);
				ui.openCorpse(null);
			}
			else{
				currentLocation.interactWithNpc(getPlayer(), ui, select.x, select.y);
			}
		}
	}
	
	public void actionSecond(UIGame ui) {
		if(ui.getMode() == Const.INVALID_ID){
			if(currentLocation.inBound(select.x, select.y)){
				GO go = currentLocation.map[select.x][select.y].go;
				if(go != null){
					if(go.proto.usable()){
						currentLocation.useGO(getPlayer(), go);
					}
					else if(go.proto.container()){
						getPlayer().containerGO(go, ui);
					}
				}
			}
		}
		else{
			ui.setMode(Const.INVALID_ID);
			Logic.playerUseSkill(null);
		}
	}
	
	public void playerSelfCastSkill(Skill skill){
		getPlayer().useSkill(currentLocation, skill, getPlayer());
	}
	
	public void playerUseSkill(UIGame ui, Skill skill) {
		getPlayer().setUsedSkill(ui, skill);
	}
	
	public Vector3 getCursorPos(){
		return cursorPos;
	}

	public Point getSelectedNode(){
		return select;
	}

	public String getSelectedCreature() {
		return currentLocation.getSelectedCreature(select.x, select.y);
	}
}