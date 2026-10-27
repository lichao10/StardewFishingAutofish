import com.bonker.stardewfishing.client.*;
import com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket;
import io.github.lichao10.stardewfishingautofish.TreasureController;
import java.nio.file.*;
import java.util.*;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

/** 1000 seeded games using the installed mod's original minigame bytecode. */
public class TreasureIntegrationTest {
    public static void main(String[] args) throws Exception {
        Field uf = Unsafe.class.getDeclaredField("theUnsafe"); uf.setAccessible(true);
        Unsafe unsafe = (Unsafe)uf.get(null);
        List<String> profiles = Files.readAllLines(Path.of(args[0]));
        StringBuilder csv = new StringBuilder("case,profile,bar_size,line_strength,treasure_bobber,chest_type,chest_position,appearance_tick,fish_caught,chest_charged,chest_landed,premature_finish,ticks,accuracy\n");
        int wins=0, chestCases=0, chests=0, charged=0, premature=0, timeouts=0;
        int gold=0, normal=0, noChestWins=0;
        int total=0, goldCases=0, normalCases=0, emptyCases=0;
        Set<Integer> selected=new HashSet<>();
        if(args.length>2 && !args[2].isBlank())
            for(String id:args[2].split(",")) selected.add(Integer.parseInt(id));
        double accuracy=0;
        long start=System.nanoTime();
        for(int index=0; index<1000; index++) {
            if(!selected.isEmpty() && !selected.contains(index))continue;
            total++;
            String[] p=profiles.get(index%profiles.size()).split(",");
            int idle=Integer.parseInt(p[1]);
            float speed=Float.parseFloat(p[2]), up=Float.parseFloat(p[3]), down=Float.parseFloat(p[4]);
            int distance=Integer.parseInt(p[5]), variation=Integer.parseInt(p[6]);
            int size=new int[]{24,36,48}[index%3];
            float strength=new float[]{0,1,1,1.5f}[index%4];
            boolean chest=index%5!=0, golden=chest && index%5<=2;
            boolean treasureBobber=index%7==0;
            Random random=new Random(20261023L+index*7919L);
            int chestPos=chest?(int)(5f+125f*random.nextFloat()):0;
            int appear=chest?(int)(20f+40f*random.nextFloat()):-1;
            FishingScreen screen=new FishingScreen();
            FishingMinigame game=(FishingMinigame)unsafe.allocateInstance(FishingMinigame.class);
            ControllerIntegrationTest.set(game,"random",random);
            ControllerIntegrationTest.set(game,"screen",screen);
            ControllerIntegrationTest.set(game,"packet",new S2CStartMinigamePacket(idle,speed,up,down,
                    distance,variation,null,chest,golden,strength,size));
            ControllerIntegrationTest.set(game,"lineStrength",strength);
            ControllerIntegrationTest.set(game,"barSize",size);
            ControllerIntegrationTest.set(game,"maxBobberHeight",142-size);
            ControllerIntegrationTest.set(game,"fishTarget",-1);
            ControllerIntegrationTest.set(game,"points",24f);
            ControllerIntegrationTest.set(game,"chestPos",chestPos);
            ControllerIntegrationTest.set(game,"chestAppearTime",appear);
            ControllerIntegrationTest.set(game,"goldenChest",golden);
            ControllerIntegrationTest.set(game,"hasTreasureBobber",treasureBobber);
            TreasureController controller=new TreasureController();
            StringBuilder trace=new StringBuilder("tick,input,bar,bar_velocity,fish,fish_velocity,points,chest_ticks,visible\n");
            int tick=0;
            for(;tick<2400 && !screen.ended; tick++) {
                boolean input=controller.choose(game.getBobberPos(),
                        ControllerIntegrationTest.f("bobberVelocity").getDouble(game),size,
                        game.getFishPos(),ControllerIntegrationTest.f("fishVelocity").getDouble(game),
                        ControllerIntegrationTest.f("fishTarget").getInt(game),
                        ControllerIntegrationTest.f("fishIsIdle").getBoolean(game),speed,up,down,
                        game.isChestVisible(),game.gotChest(),game.getChestPos(),game.getChestProgress(),
                        game.getProgress(),strength,treasureBobber);
                if(!selected.isEmpty())trace.append(String.format(Locale.ROOT,
                        "%d,%s,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%s%n",tick,input,
                        game.getBobberPos(),ControllerIntegrationTest.f("bobberVelocity").getDouble(game),
                        game.getFishPos(),ControllerIntegrationTest.f("fishVelocity").getDouble(game),
                        game.getProgress()*120,game.getChestProgress()*30,game.isChestVisible()));
                game.tick(input);
                if(game.getBobberPos()<0 || game.getBobberPos()>142-size)
                    throw new AssertionError("bar out of bounds");
            }
            if(screen.won) { wins++; accuracy+=screen.accuracy; }
            if(chest) {
                chestCases++;
                if(golden)goldCases++;else normalCases++;
                if(game.gotChest()) charged++;
                if(screen.won && screen.chest) { chests++; if(golden)gold++; else normal++; }
                if(screen.won && !screen.chest)premature++;
            } else { emptyCases++; if(screen.won)noChestWins++; }
            if(!screen.ended)timeouts++;
            csv.append(String.format(Locale.ROOT,"%d,%s,%d,%.1f,%s,%s,%d,%d,%s,%s,%s,%s,%d,%.5f%n",
                    index,p[0],size,strength,treasureBobber,chest?(golden?"golden":"normal"):"none",
                    chestPos,appear,screen.won,game.gotChest(),screen.won&&screen.chest,
                    chest&&screen.won&&!screen.chest,tick,screen.accuracy));
            if(!selected.isEmpty())Files.writeString(Path.of(args[1]+".case-"+index+".csv"),trace);
        }
        Files.writeString(Path.of(args[1]),csv);
        String summary=String.format(Locale.ROOT,
                "%d actual SF 3.7 games: fish=%d/%d; chests=%d/%d (normal=%d/%d, golden=%d/%d); charged=%d; premature=%d; no-chest fish=%d/%d; timeouts=%d; mean winning accuracy=%.2f%%; elapsed=%.2fs%n",
                total,wins,total,chests,chestCases,normal,normalCases,gold,goldCases,charged,premature,noChestWins,emptyCases,timeouts,
                accuracy/Math.max(1,wins)*100,(System.nanoTime()-start)/1e9);
        System.out.print(summary);
        Files.writeString(Path.of(args[1]+".summary.txt"),summary);
        if(timeouts>0)System.out.println("NOTE: timeout cases require investigation; see CSV.");
        if(selected.isEmpty() && (total!=1000 || wins<980 || chests<760
                || premature!=0 || timeouts!=0))
            throw new AssertionError("Treasure strategy regression; inspect the recorded cases.");
    }
}
