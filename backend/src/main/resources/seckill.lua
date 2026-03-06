-- 秒杀库存扣减Lua脚本
-- 使用Lua脚本保证Redis操作的原子性
-- 避免超卖问题

-- 获取库存key
local stockKey = KEYS[1]

-- 获取当前库存
local stock = redis.call('get', stockKey)

-- 判断库存是否充足
if stock == false or tonumber(stock) <= 0 then
    -- 库存不足，返回-1
    return -1
end

-- 扣减库存
redis.call('decr', stockKey)

-- 返回剩余库存
return tonumber(stock) - 1
