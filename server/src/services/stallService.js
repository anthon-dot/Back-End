import { supabase } from '../config/supabase.js';
import { STALL_STATUS } from '../config/constants.js';

export async function getAllStalls(filter = {}) {
  let query = supabase
    .from('stalls')
    .select('*, section:sections(*), occupant:occupants(*, stakeholder:stakeholders(*))')
    .order('stall_no', { ascending: true });

  if (filter.status) {
    query = query.eq('status', filter.status);
  }
  if (filter.sectionId) {
    query = query.eq('section_id', filter.sectionId);
  }

  const { data, error } = await query;
  if (error) throw error;
  return data || [];
}

export async function getStallById(id) {
  const { data, error } = await supabase
    .from('stalls')
    .select('*, section:sections(*), occupant:occupants(*, stakeholder:stakeholders(*))')
    .eq('id', Number(id))
    .single();

  if (error) throw error;
  return data;
}

export async function updateStallMapPosition(id, { mapCoordinates, latitude, longitude, dimensions }) {
  const updatePayload = {
    updated_at: new Date().toISOString()
  };
  if (mapCoordinates) updatePayload.map_coordinates = mapCoordinates;
  if (latitude !== undefined) updatePayload.latitude = latitude;
  if (longitude !== undefined) updatePayload.longitude = longitude;
  if (dimensions) updatePayload.dimensions = dimensions;

  const { data, error } = await supabase
    .from('stalls')
    .update(updatePayload)
    .eq('id', Number(id))
    .select()
    .single();

  if (error) throw error;
  return data;
}

export async function reserveStallAtomically(stallId, occupantId) {
  // Check if stall is available
  const { data: currentStall, error: getErr } = await supabase
    .from('stalls')
    .select('id, status, occupant_id, stall_no')
    .eq('id', Number(stallId))
    .single();

  if (getErr || !currentStall) {
    throw new Error('Stall not found for reservation.');
  }

  if (currentStall.status === STALL_STATUS.OCCUPIED) {
    throw new Error(`Stall #${currentStall.stall_no} is already occupied.`);
  }

  const { data: updated, error: updErr } = await supabase
    .from('stalls')
    .update({
      status: STALL_STATUS.RESERVED,
      occupant_id: occupantId || null,
      updated_at: new Date().toISOString()
    })
    .eq('id', Number(stallId))
    .select()
    .single();

  if (updErr) throw updErr;
  return updated;
}
