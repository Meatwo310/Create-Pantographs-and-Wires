package de.mrjulsen.paw.block.abstractions;

import java.util.Optional;

import de.mrjulsen.paw.block.extended.BlockPlaceContextExtension;
import de.mrjulsen.paw.block.property.ECantileverConnectionType;
import de.mrjulsen.paw.blockentity.CantileverBlockEntity;
import de.mrjulsen.paw.client.gui.ModGuiIcons;
import de.mrjulsen.paw.client.gui.widgets.IIconEnum;
import de.mrjulsen.paw.registry.ModBlockEntities;
import de.mrjulsen.paw.registry.ModBlocks;
import de.mrjulsen.paw.util.Utils;
import de.mrjulsen.mcdragonlib.core.ITranslatableEnum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractCantileverBlock extends AbstractRotatableWireConnectorBlock<CantileverBlockEntity> implements ICatenaryWireConnector {

    public static final byte MIN_SIZE = 3;
    public static final byte MAX_SIZE = 7;

    public static enum ECantileverRegistrationArmType implements StringRepresentable, IIconEnum, ITranslatableEnum {
        CENTER("center", ModGuiIcons.CANTILEVER_CENTER),
        INNER("inner", ModGuiIcons.CANTILEVER_INNER),
        OUTER("outer", ModGuiIcons.CANTILEVER_OUTER);

        final String name;
        final ModGuiIcons icon;

        ECantileverRegistrationArmType(String name, ModGuiIcons icon) {
            this.name = name;
            this.icon = icon;
        }

        @Override
        public ModGuiIcons getIcon() {
            return icon;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String getEnumName() {
            return "cantilever_registration_arm";
        }

        @Override
        public String getEnumValueName() {
            return name;
        }

        public static ECantileverRegistrationArmType def() {
            return CENTER;
        }
    }

    public static enum ECantileverInsulatorsPlacement implements StringRepresentable, IIconEnum, ITranslatableEnum {
        BACK("back", ModGuiIcons.CANTILEVER_INSULATOR_BACK),
        FRONT("front", ModGuiIcons.CANTILEVER_INSULATOR_FRONT);

        final String name;
        final ModGuiIcons icon;

        ECantileverInsulatorsPlacement(String name, ModGuiIcons icon) {
            this.name = name;
            this.icon = icon;
        }

        @Override
        public ModGuiIcons getIcon() {
            return icon;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        @Override
        public String getEnumName() {
            return "insulator_placement";
        }

        @Override
        public String getEnumValueName() {
            return name;
        }

        public static ECantileverInsulatorsPlacement def() {
            return BACK;
        }
    }

    public static final String NBT_TENSION_WIRE_ATTACH_POINT = "TensionWireAttachPoint";

    public static final EnumProperty<ECantileverConnectionType> CONNECTION = EnumProperty.create("connection", ECantileverConnectionType.class);
    public static final EnumProperty<ECantileverRegistrationArmType> REGISTRATION_ARM = EnumProperty.create("registration_arm", ECantileverRegistrationArmType.class);
    public static final EnumProperty<ECantileverInsulatorsPlacement> INSULATORS_PLACEMENT = EnumProperty.create("insulator_placement", ECantileverInsulatorsPlacement.class);

    public AbstractCantileverBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL)
            .noOcclusion()
        );

        this.registerDefaultState(this.defaultBlockState()
            .setValue(CONNECTION, ECantileverConnectionType.PX16)
            .setValue(REGISTRATION_ARM, ECantileverRegistrationArmType.def())
            .setValue(INSULATORS_PLACEMENT, ECantileverInsulatorsPlacement.def())
        );
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {        
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(CONNECTION, REGISTRATION_ARM, INSULATORS_PLACEMENT);
    }

    @Override
    public Class<CantileverBlockEntity> getBlockEntityClass() {
        return CantileverBlockEntity.class;
    }

    public BlockState copyProperties(BlockState src, BlockState target) {        
        if (!(src.getBlock() instanceof AbstractCantileverBlock) || !(target.getBlock() instanceof AbstractCantileverBlock)) {
            throw new IllegalArgumentException("One of the given blockstates is no AbstractCantileverBlock. src: " + src + ", target: " + target);
        }

        for (Property<?> property : src.getBlock().getStateDefinition().getProperties()) {
            if (!target.hasProperty(property))
				continue;

			target = copyPropertyOf(src, target, property);
        }
        return target;
    }

	protected static <T extends Comparable<T>> BlockState copyPropertyOf(BlockState sourceState, BlockState targetState, Property<T> property) {
        return (BlockState)targetState.setValue(property, sourceState.getValue(property));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPlaceContextExtension ctxExt = (BlockPlaceContextExtension)(Object)context;
        Direction direction = context.getClickedFace();
        BlockPos clickPos = context.getClickedPos().relative(direction.getOpposite());
        Level level = context.getLevel();
        BlockState clickedState = level.getBlockState(clickPos);

        BlockState state = super.defaultBlockState();
        Direction targetDir = direction;
        int targetRotationIdx = 0;
        Optional<ECantileverConnectionType> connectionType = ECantileverConnectionType.getFirstForState(ctxExt.getPlacedOnState());

        if (direction.getAxis() != Axis.Y &&
            clickedState.getBlock() instanceof AbstractRotatableBlock
        ) {
            targetDir = direction;
            targetRotationIdx = clickedState.getValue(ROTATION);
        } else if (direction.getAxis() != Axis.Y && (connectionType.isPresent() || ctxExt.getPlacedOnState().isFaceSturdy(level, ctxExt.getPlacedOnPos(), context.getClickedFace()))) {
            targetDir = context.getClickedFace();
            targetRotationIdx = 1;
        } else {
            int rot = (Mth.floor((double)((180.0F + context.getRotation()) * (float)TOTAL_ROTATION_STEPS / 360.0F) + 0.5) & (TOTAL_ROTATION_STEPS - 1));
            final int steps = TOTAL_ROTATION_STEPS / 4;
            Direction dir = Direction.from2DDataValue((rot + (steps / 2)) / 4);

            targetDir = dir;
            targetRotationIdx = steps - 1 - ((rot + (steps / 2)) % 4);
        }

        state = state
            .setValue(FACING, targetDir)
            .setValue(ROTATION, targetRotationIdx)
            .setValue(CONNECTION, connectionType.orElse(ECantileverConnectionType.PX16))
        ;
        return state;        
    }    

    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos currentPos, BlockPos neighborPos) {
        return !this.canSurvive(state, level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction direction = state.getValue(FACING);
        BlockPos relativePos = pos.relative(direction.getOpposite());
        if (getRelativeYRotation(state) > 30 && level.getBlockState(pos).getBlock() instanceof AbstractCantileverBlock) {
            relativePos = relativePos.relative(direction.getClockWise());
        }        
        BlockState supportState = level.getBlockState(relativePos);
        return
            (supportState.is(getSupportBlockTag()) || supportState.isFaceSturdy(level, relativePos, direction.getOpposite())) &&
            (supportState.getBlock() instanceof AbstractRotatableBlock ? (int)getRelativeYRotation(supportState) : 0) == (int)getRelativeYRotation(state)
        ;
    }

    protected TagKey<Block> getSupportBlockTag() {
        return ModBlocks.TAG_CANTILEVER_CONNECTABLE;
    }

    @Override
    public VoxelShape getBaseShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double stretch = 16d * ((1d / Math.cos(Math.abs(Math.toRadians(getRelativeYRotation(state))))) - 1d);
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(6d, 0, 0, 10d, 16d, 16d + stretch);
            case WEST  -> Block.box(-stretch, 0, 6d, 16d, 16d, 10d);
            case EAST  -> Block.box(0, 0, 6d, 16d + stretch, 16d, 10d);
            default    -> Block.box(6d, 0, -stretch, 10d, 16d, 16d);
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntityType<? extends CantileverBlockEntity> getBlockEntityType() {
       return ModBlockEntities.CANTILEVER_BLOCK_ENTITY.get();
    }

    @Override
    public Vec2 getRotationPivotPoint(BlockGetter level, BlockPos pos, BlockState state) {
        return new Vec2(0f, 1f);
    }

    @Override
    public Vec2 getOffset(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.getValue(ROTATION) < PROPERTY_MAX_ROTATION_INDEX) {
            return new Vec2(0, 0);
        }
        return switch (state.getValue(FACING)) {
            case WEST  -> new Vec2(0, -1);
            case EAST  -> new Vec2(0, 1);
            case SOUTH -> new Vec2(-1, 0);
            default    -> new Vec2(1, 0);
        };
    }

    @Override
    public CompoundTag wireRenderData(Level level, BlockPos pos, BlockState state, CompoundTag itemData, boolean firstPoint) {
        CompoundTag nbt = super.wireRenderData(level, pos, state, itemData, firstPoint);
        Utils.putNbtVec3(nbt, NBT_TENSION_WIRE_ATTACH_POINT, transformWireAttachPoint(level, pos, state, itemData, firstPoint, this::tensionWireAttachPoint));
        return nbt;
    }

    public abstract Vec3 multiblockSize();
}
