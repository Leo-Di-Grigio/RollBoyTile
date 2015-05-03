package game.script.game.effect;

import game.state.location.LocationObject;
import game.state.location.creature.Creature;
import game.state.skill.SkillEffect;

public class effect_Heal implements SkillEffect {

	private int heal;
	
	public effect_Heal(int heal) {
		this.heal = heal;
	}

	@Override
	public void execute(LocationObject caster, LocationObject target) {
		if(target.isCreature()){
			Creature creature = (Creature)target;
			
			if(creature.struct().isAlive()){
				creature.struct().heal(heal);
			}
		}
	}
}