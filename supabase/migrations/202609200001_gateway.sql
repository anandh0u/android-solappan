-- Private beta: no public signup can spend model credits until explicitly enabled.
create table public.sol_beta_users (
  user_id uuid primary key references auth.users(id) on delete cascade,
  enabled boolean not null default false
);
create table public.sol_usage (
  bucket text primary key,
  requests integer not null default 0,
  expires_at timestamptz not null
);
create table public.sol_response_owners (
  response_id text primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  expires_at timestamptz not null default now() + interval '24 hours'
);
alter table public.sol_beta_users enable row level security;
alter table public.sol_usage enable row level security;
alter table public.sol_response_owners enable row level security;
revoke all on public.sol_beta_users, public.sol_usage, public.sol_response_owners from anon, authenticated;
grant all on public.sol_beta_users, public.sol_usage, public.sol_response_owners to service_role;
create index sol_response_expiry on public.sol_response_owners(expires_at);

create or replace function public.sol_reserve_request(p_user uuid)
returns boolean language plpgsql security definer set search_path = '' as $$
declare v_now timestamptz := now(); v_count integer; v_bucket text; v_limit integer; i integer;
begin
  -- Serializes quota reservation across all edge workers. Small private-beta throughput.
  perform pg_advisory_xact_lock(78263142);
  if not exists(select 1 from public.sol_beta_users where user_id = p_user and enabled) then
    return false;
  end if;
  delete from public.sol_usage where expires_at < v_now;
  delete from public.sol_response_owners where expires_at < v_now;
  for i in 1..3 loop
    v_bucket := case i
      when 1 then 'minute:' || p_user || ':' || to_char(v_now at time zone 'UTC','YYYYMMDDHH24MI')
      when 2 then 'day:' || p_user || ':' || to_char(v_now at time zone 'UTC','YYYYMMDD')
      else 'global:' || to_char(v_now at time zone 'UTC','YYYYMMDD') end;
    v_limit := case i when 1 then 12 when 2 then 100 else 1000 end;
    select requests into v_count from public.sol_usage where bucket=v_bucket;
    if coalesce(v_count,0) >= v_limit then return false; end if;
  end loop;
  for i in 1..3 loop
    v_bucket := case i
      when 1 then 'minute:' || p_user || ':' || to_char(v_now at time zone 'UTC','YYYYMMDDHH24MI')
      when 2 then 'day:' || p_user || ':' || to_char(v_now at time zone 'UTC','YYYYMMDD')
      else 'global:' || to_char(v_now at time zone 'UTC','YYYYMMDD') end;
    insert into public.sol_usage(bucket,requests,expires_at) values(v_bucket,1,v_now+interval '25 hours')
    on conflict(bucket) do update set requests=public.sol_usage.requests+1;
  end loop;
  return true;
end $$;
revoke all on function public.sol_reserve_request(uuid) from public, anon, authenticated;
grant execute on function public.sol_reserve_request(uuid) to service_role;
