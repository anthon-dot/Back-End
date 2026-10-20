import { getVerifiedReportMetrics } from '../services/reportService.js';

export async function getDashboardReports(req, res, next) {
  try {
    const reportData = await getVerifiedReportMetrics();
    return res.json({
      success: true,
      report: reportData
    });
  } catch (err) {
    next(err);
  }
}
