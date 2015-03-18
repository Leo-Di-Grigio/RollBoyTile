package game.cycle.scene.game.world.creature.ai;

import java.awt.Point;

import game.cycle.scene.game.world.creature.Creature;
import game.cycle.scene.game.world.creature.NPC;
import game.cycle.scene.game.world.map.Location;
import game.cycle.scene.game.world.map.Terrain;

// ������������
public class AI {
	
	public static void execute(Location loc, NPC agent){
		agent.aidata.executed = true;
		agent.aidata.clear();
		
		if(agent.ap == 0){
			agent.aidata.updated = true;
			return;
		}
		else{
			reciveSensorData(loc, agent);

			if(agent.aidata.viewedEnemy.size() > 0){
				attack(loc, agent);
			}
			else{
				agent.aidata.updated = true;
			}
		}
	}
	
	public static void update(Location loc, NPC agent){
		if(agent.ap == 0){
			agent.aidata.updated = true;
		}
		else{
			if(agent.aidata.viewedEnemy.size() == 0){
				agent.aidata.updated = true;
			}
			else{
				if(agent.getPath() == null){
					execute(loc, agent);
				}
			}
		}
	}
	
	private static void reciveSensorData(Location loc, NPC agent){
		Terrain [][] map = loc.map;
		
		Point pos = agent.getAbsolutePosition();
		int x = pos.x;
		int y = pos.y;
		int r = agent.proto.stats.perception;
		
		int xmin = Math.max(x - r, 0);
		int ymin = Math.max(y - r, 0);
		int xmax = Math.min(x + r, loc.proto.sizeX - 1);
		int ymax = Math.min(y + r, loc.proto.sizeY - 1);
	
		for(int i = xmin; i < xmax; ++i){
			for(int j = ymin; j < ymax; ++j){
				if(map[i][j].creature != null && map[i][j].creature.id != agent.id){
					agent.aidata.addView(map[i][j].creature);
				}
			}
		}
	}

	private static void attack(Location loc, NPC agent) {
		float minRange = 100.0f;
		Creature nearEnemy = null;
		
		for(Creature enemy: agent.aidata.viewedEnemy.values()){
			float range = loc.getRange(agent, enemy);
			if(range < minRange){
				minRange = range;
				nearEnemy = enemy;
			}
		}
		
		Point pos = nearEnemy.getPosition();
		if(minRange < agent.skills.attack.range){
			// attack
			while(loc.useSkill(agent.skills.attack, agent, pos.x, pos.y));
			agent.aidata.updated = true;
		}
		else{
			// follow
			agent.move(loc.map, loc.proto.sizeX, loc.proto.sizeY, pos.x, pos.y);
			if(agent.getPath() == null){
				agent.aidata.updated = true;
			}
		}
	}
}
