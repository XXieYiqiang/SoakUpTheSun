package org.hgc.suts.gateway.chain;

import lombok.Setter;

/**
 * 责任链节点抽象基类
 */
public abstract class AbstractChainNode {

    /**
     * 下一个节点
     */
    @Setter
    protected AbstractChainNode nextNode;

    /**
     * 节点执行入口
     */
    public void doChain(AiChatContext context) {
        // 1. 执行当前节点的逻辑

        execute(context);

        // 2. 如果有下一个节点，继续传递
        if (nextNode != null) {
            nextNode.doChain(context);
        }
    }

    /**
     * 核心业务逻辑 (由具体节点实现)
     */
    protected abstract void execute(AiChatContext context);
}