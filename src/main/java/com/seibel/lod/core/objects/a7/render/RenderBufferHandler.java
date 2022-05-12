package com.seibel.lod.core.objects.a7.render;

import com.seibel.lod.core.objects.Pos2D;
import com.seibel.lod.core.objects.a7.LodQuadTree;
import com.seibel.lod.core.objects.a7.LodSection;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.objects.opengl.RenderBuffer;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.gridList.MovableGridRingList;

import java.util.concurrent.atomic.AtomicReference;

public class RenderBufferHandler {
    public final LodQuadTree target;
    private final MovableGridRingList<RenderBufferNode> renderBufferNodes;

    class RenderBufferNode implements AutoCloseable {
        public final DhSectionPos pos;
        public volatile RenderBufferNode[] children = null;
        public AtomicReference<RenderBuffer> renderBufferSlot = null;

        public RenderBufferNode(DhSectionPos pos) {
            this.pos = pos;
        }

        public void render(LodRenderProgram renderContext) {
            RenderBuffer buff = renderBufferSlot.get();
            if (buff != null) {
                buff.render(renderContext);
            } else {
                RenderBufferNode[] childs = children;
                if (childs != null) {
                    for (RenderBufferNode child : childs) {
                        child.render(renderContext);
                    }
                }
            }
        }

        //TODO: In the future make this logic a bit more complex so that when children are just created,
        //      the buffer is only unloaded if all children's buffers are ready. This will make the
        //      transition between buffers no longer causing any flicker.
        public void update() {
            LodSection section = target.getSection(pos);
            // If this fails, there may be concurrent modification of the quad tree
            //  (as this update() should be called from the same thread that calls update() on the quad tree)
            LodUtil.assertTrue(section != null);
            RenderDataSource container = section.getRenderContainer();

            // Update self's render buffer state
            boolean shouldRender = section.isLoaded();
            if (!shouldRender) {
                RenderBuffer buff = renderBufferSlot.getAndSet(null);
                if (buff != null) {
                    buff.close();
                }
            } else {
                container.trySwapRenderBuffer(renderBufferSlot);
            }

            // Update children's render buffer state
            boolean shouldHaveChildren = !section.isLoaded();
            if (shouldHaveChildren) {
                if (children == null) {
                    RenderBufferNode[] childs = new RenderBufferNode[4];
                    for (int i = 0; i < 4; i++) {
                        childs[i] = new RenderBufferNode(pos.getChild(i));
                    }
                    children = childs;
                }
                for (RenderBufferNode child : children) {
                    child.update();
                }
            } else {
                if (children != null) {
                    RenderBufferNode[] childs = children;
                    children = null;
                    for (RenderBufferNode child : childs) {
                        child.close();
                    }
                }
            }
        }

        @Override
        public void close() {
            if (children != null) {
                for (RenderBufferNode child : children) {
                    child.close();
                }
            }
            RenderBuffer buff = renderBufferSlot.getAndSet(null);
            if (buff != null) {
                buff.close();
            }
        }
    }

    public RenderBufferHandler(LodQuadTree target) {
        this.target = target;
        MovableGridRingList<LodSection> referenceList = target.getRingList((byte) (target.getNumbersOfDetailLevels() - 1));
        Pos2D center = referenceList.getCenter();
        renderBufferNodes = new MovableGridRingList<>(referenceList.getHalfSize(), center);
    }

    public void render(LodRenderProgram renderContext) {
        //TODO: This might get locked by update() causing move() call. Is there a way to avoid this?
        // Maybe dupe the base list and use atomic swap on render? Or is this not worth it?
        renderBufferNodes.forEachOrdered(n -> n.render(renderContext));
    }

    public void update() {
        byte topDetail = (byte) (target.getNumbersOfDetailLevels() - 1);
        MovableGridRingList<LodSection> referenceList = target.getRingList(topDetail);
        Pos2D center = referenceList.getCenter();
        renderBufferNodes.move(center.x, center.y, RenderBufferNode::close); // Note: may lock the list
        renderBufferNodes.forEachPosOrdered((node, pos) -> {
            DhSectionPos sectPos = new DhSectionPos(topDetail, pos.x, pos.y);
            LodSection section = target.getSection(sectPos);

            if (section == null) {
                // If section is null, but node exists, remove node
                if (node != null) {
                    renderBufferNodes.remove(pos).close();
                }
                // If section is null, continue
                return;
            }

            // If section is not null, but node does not exist, create node
            if (node == null) {
                node = renderBufferNodes.setChained(pos, new RenderBufferNode(sectPos));
            }
            // Node should be not null here
            // Update node
            node.update();
        });
    }

    public void close() {
        renderBufferNodes.clear(RenderBufferNode::close);
    }
}
