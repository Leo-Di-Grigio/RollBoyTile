package game.state.location.creature.ai;

import java.awt.Point;

import com.badlogic.gdx.Gdx;

import tools.Const;
import tools.Tools;
import game.state.event.Event;
import game.state.location.Location;
import game.state.location.LocationObject;
import game.state.location.Node;
import game.state.location.creature.Creature;
import game.state.location.creature.NPC;
import game.state.location.go.GO;

public class AI {
	
	public static void fullUpdate(Location loc, NPC agent){
		agent.aidata.fullUpdate = true;
		agent.aidata.clear();
		
		if(agent.ap == 0){
			agent.aidata.softUpdated = true;
		}
		else{
			reciveSensorData(loc, agent);
			
			if(agent.aidata.viewedEnemy.size() == 0){
				moveToWayPoint(loc, agent);
			}
			else{
				attack(loc, agent);
			}
		}
	}

	public static void softUpdate(Location loc, NPC agent){
		if(agent.ap == 0){
			agent.aidata.softUpdated = true;
		}
		else{
			if(agent.aidata.viewedEnemy.size() == 0){
				agent.aidata.softUpdated = true;
			}
			else{
				if(agent.getPath() == null){
					fullUpdate(loc, agent);
				}
			}
		}
	}

	public static void event(Location loc, Event event, NPC agent) {
		float r1, r2;
		
		if(event.target == null || event.source == null){
			r1 = Const.AI_CALCULATE_RANGE;
			r2 = Const.AI_CALCULATE_RANGE;
		}
		else{
			r1 = Tools.getRange(agent, event.source);
			r2 = Tools.getRange(agent, event.target);
		}
	
		if(r1 <= Const.AI_CALCULATE_RANGE || r2 <= Const.AI_CALCULATE_RANGE){
			switch (event.type) {
	
				case Event.EVENT_VISUAL_ATTACK:
					eventVisualAttack(loc, event, agent);
					break;
				
				case Event.EVENT_SOUND_ATTACK:
					eventSoundAttack(loc, event, agent);
					break;
				
				case Event.EVENT_DIALOG_BEGIN:
					eventDialogBegin(loc, event, agent);
					break;
				
				case Event.EVENT_DIALOG_END:
					eventDialogEnd(loc, event, agent);
					break;
				
				default:
					break;
			}
		}
	}

	private static void eventVisualAttack(Location loc, Event event, NPC agent) {
		if(event.target.fraction == agent.fraction){
			if(event.source.isCreature()){
				if(AITools.isVisible(loc, agent, event.source)){
					agent.aidata.addEnemy((Creature)event.source);
					agent.aidata.combat = true;
					agent.aidata.fullUpdate = false;
				}
			}
		}
	}

	private static void eventSoundAttack(Location loc, Event event, NPC agent) {
		if(Perception.isHear(agent, AITools.getVolume(loc, agent, event))){
			agent.aidata.addPointOfInteres(event.source.getPosition().x, event.source.getPosition().y);
		}
	}
	
	private static void eventDialogBegin(Location loc, Event event, NPC agent) {

	}
	
	private static void eventDialogEnd(Location loc, Event event, NPC agent) {

	}
	
	private static void reciveSensorData(Location loc, NPC agent){
		Node [][] map = loc.map;
		
		Point pos = agent.getPosition();
		int x = pos.x;
		int y = pos.y;
		int r = agent.proto().stats().perception*2;
		
		int xmin = Math.max(x - r, 0);
		int ymin = Math.max(y - r, 0);
		int xmax = Math.min(x + r, loc.proto.sizeX() - 1);
		int ymax = Math.min(y + r, loc.proto.sizeY() - 1);
		
		Creature target = null;
		
		for(int i = xmin; i <= xmax; ++i){
			for(int j = ymin; j <= ymax; ++j){
				// Check node [i][j]
				if(map[i][j].creature != null){
					target = map[i][j].creature;
				
					if(target.getGUID() != agent.getGUID() && AITools.isVisible(loc, agent, target)){
						// Find another Creature
						agent.aidata.addViewedCreature(target);
						
						// Corpse find (body)
						if(!target.isAlive() && target.proto().fraction() == agent.proto().fraction()){
							agent.aidata.foundCorpse = true;
						}
						
						// Corpse find (dragged)
						if(target.getDraggedObject() != null){
							LocationObject draggedObject = target.getDraggedObject();
							
							if(draggedObject.isCreature()){
								// Yep, this is corpse
								Creature draggedCorpse = (Creature)draggedObject;
								
								if(draggedCorpse.proto().fraction() == agent.proto().fraction()){
									agent.aidata.foundCorpse = true;
									agent.aidata.addEnemy(target);
								}
							}
						}
					}
				}
			}
		}
	}

	private static void attack(Location loc, NPC agent) {
		if(loc.isTurnBased()){
			agent.aidata.combat = true;
			
			float minRange = Float.MAX_VALUE;
			Creature nearestEnemy = null;
			
			for(Creature enemy: agent.aidata.viewedEnemy.values()){
				float range = Tools.getRange(agent, enemy);
				
				if(range < minRange){
					minRange = range;
					nearestEnemy = enemy;
				}
			}
			
			Point pos = nearestEnemy.getPosition();
			
			if(minRange <= agent.skills().get(0).range){
				// attack
				while(agent.useSkill(loc, agent.skills().get(0), pos.x, pos.y));
				agent.aidata.softUpdated = true;
			}
			else{
				// follow
				agent.move(loc, pos.x, pos.y);
				
				if(agent.getPath() == null){
					agent.aidata.softUpdated = true;
				}
			}	
		}
	}
	
	private static void moveToWayPoint(Location loc, NPC agent) {
		if(loc.isTurnBased() && agent.aidata.waypointPause < agent.aidata.waypointPauseMax){
			agent.aidata.waypointPause++;
			agent.aidata.fullUpdate = true;
		}
		else if(!loc.isTurnBased() && agent.aidata.waypointPauseSec < agent.aidata.waypointPauseSecMax){
			agent.aidata.waypointPauseSec += Gdx.graphics.getDeltaTime();
			agent.aidata.softUpdated = true;
		}
		else{
			GO wp = agent.aidata.getNextWayPoint();
		
			if(wp == null){
				agent.aidata.softUpdated = true;
			}
			else{
				int waypointPause = agent.aidata.getWayPointPause(wp.getGUID());
				agent.aidata.waypointPause = 0;
				agent.aidata.waypointPauseSec = 0.0f;
				agent.aidata.waypointPauseMax = waypointPause;
				agent.aidata.waypointPauseSecMax = waypointPause;
				
				agent.move(loc, wp.getPosition().x, wp.getPosition().y);
		
				if(agent.getPath() == null){
					agent.aidata.softUpdated = true;
				}
			}
		}
	}
}