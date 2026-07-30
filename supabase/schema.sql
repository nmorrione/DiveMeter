-- DiveMeter shared database schema
-- Run this once in the Supabase SQL Editor (Project -> SQL Editor -> New query).

-- One row per device identity (backed by Supabase anonymous auth), holding the
-- globally unique nickname shown to other users.
create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  nickname text not null unique,
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;

create policy "Profiles are viewable by everyone"
  on public.profiles for select
  using (true);

create policy "Users can insert their own profile"
  on public.profiles for insert
  with check (auth.uid() = id);

create policy "Users can update their own profile"
  on public.profiles for update
  using (auth.uid() = id)
  with check (auth.uid() = id);

-- The shared dive spots. Readable by everyone; only the owner (matched via the
-- server-verified auth identity, not the nickname text) can delete their own.
create table public.dives (
  id bigint generated always as identity primary key,
  owner_id uuid not null references auth.users(id) on delete cascade,
  owner_nickname text not null,
  spot_name text not null,
  height_meters double precision not null,
  latitude double precision not null,
  longitude double precision not null,
  method text not null, -- 'MANUAL' | 'VIDEO' | 'BAROMETER'
  description text not null default '',
  rating integer not null default 0 check (rating between 0 and 5),
  created_at timestamptz not null default now()
);

alter table public.dives enable row level security;

create policy "Dives are viewable by everyone"
  on public.dives for select
  using (true);

create policy "Users can insert their own dives"
  on public.dives for insert
  with check (auth.uid() = owner_id);

create policy "Users can delete their own dives"
  on public.dives for delete
  using (auth.uid() = owner_id);
