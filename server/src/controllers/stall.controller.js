import { getAllStalls, getStallById, updateStallMapPosition } from '../services/stallService.js';
import { supabase } from '../config/supabase.js';

export async function listStalls(req, res, next) {
  try {
    const { status, sectionId } = req.query;
    const stalls = await getAllStalls({ status, sectionId });
    return res.json({ success: true, stalls });
  } catch (err) {
    next(err);
  }
}

export async function getStall(req, res, next) {
  try {
    const { id } = req.params;
    const stall = await getStallById(id);
    return res.json({ success: true, stall });
  } catch (err) {
    next(err);
  }
}

export async function updateMapPosition(req, res, next) {
  try {
    const { id } = req.params;
    const { mapCoordinates, latitude, longitude, dimensions } = req.body;
    const updated = await updateStallMapPosition(id, { mapCoordinates, latitude, longitude, dimensions });
    return res.json({ success: true, stall: updated });
  } catch (err) {
    next(err);
  }
}

export async function listSections(req, res, next) {
  try {
    const { data, error } = await supabase.from('sections').select('*').order('name');
    if (error) throw error;
    return res.json({ success: true, sections: data || [] });
  } catch (err) {
    next(err);
  }
}

export async function listRates(req, res, next) {
  try {
    const { data, error } = await supabase.from('rental_rates').select('*, section:sections(*)').order('id');
    if (error) throw error;
    return res.json({ success: true, rates: data || [] });
  } catch (err) {
    next(err);
  }
}
