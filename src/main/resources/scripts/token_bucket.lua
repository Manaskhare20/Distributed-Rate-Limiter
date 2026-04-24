-- KEYS[1] - state key for this user + endpoint
-- ARGV[1] - capacity (max tokens)
-- ARGV[2] - refill rate (tokens per minute)
-- ARGV[3] - current time in milliseconds

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now_ms = tonumber(ARGV[3])

local state = redis.call('HMGET', key, 'tokens', 'last_refill_ts')
local tokens = tonumber(state[1])
local last_refill = tonumber(state[2])

if tokens == nil or last_refill == nil then
  tokens = capacity
  last_refill = now_ms
end

local elapsed_ms = now_ms - last_refill
if elapsed_ms < 0 then
  elapsed_ms = 0
end

if refill_rate > 0 and elapsed_ms > 0 then
  -- convert tokens per minute to tokens per millisecond
  local refill_per_ms = refill_rate / 60000.0
  local tokens_to_add = math.floor(elapsed_ms * refill_per_ms)
  if tokens_to_add > 0 then
    tokens = math.min(capacity, tokens + tokens_to_add)
    last_refill = now_ms
  end
end

local allowed = 0
local retry_after_ms = 0

if tokens > 0 then
  tokens = tokens - 1
  allowed = 1
else
  allowed = 0
  if refill_rate > 0 then
    local refill_per_ms = refill_rate / 60000.0
    retry_after_ms = math.ceil(1 / refill_per_ms)
  else
    retry_after_ms = -1
  end
end

redis.call('HMSET', key, 'tokens', tokens, 'last_refill_ts', last_refill)
-- Set TTL for bucket state to 24 hours (86,400,000 ms)
redis.call('PEXPIRE', key, 86400000)

return {allowed, tokens, retry_after_ms}

