import React, { useEffect, useState, useCallback } from 'react';
import { FiPlus, FiChevronLeft, FiChevronRight } from 'react-icons/fi';
import { toast } from 'react-toastify';
import routeService from '../../services/routeService';
import DataTable from '../../components/common/DataTable';
import Modal from '../../components/common/Modal';
import ConfirmDialog from '../../components/common/ConfirmDialog';
import RouteForm from './RouteForm';

function RouteList() {
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editRoute, setEditRoute] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  const columns = [
    { key: 'origin', label: 'Origin' },
    { key: 'destination', label: 'Destination' },
    { key: 'distance', label: 'Distance (km)' },
    { key: 'estimatedTimeMinutes', label: 'Est. Time (min)' },
    { key: 'active', label: 'Active', render: (v) => v ? '✓' : '✗' },
    { key: 'description', label: 'Description' },
  ];

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const res = await routeService.getAll({ page, size: pageSize });
      setRoutes(res.data.content);
      setTotalPages(res.data.totalPages);
      setTotalElements(res.data.totalElements);
    } catch (err) {
      toast.error('Failed to load routes');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleCreate = () => { setEditRoute(null); setModalOpen(true); };
  const handleEdit = (route) => { setEditRoute(route); setModalOpen(true); };

  const handleSubmit = async (data) => {
    try {
      if (editRoute) {
        await routeService.update(editRoute.id, data);
        toast.success('Route updated');
      } else {
        await routeService.create(data);
        toast.success('Route created');
      }
      setModalOpen(false);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Operation failed');
    }
  };

  const handleDelete = async () => {
    try {
      await routeService.delete(deleteTarget.id);
      toast.success('Route deleted');
      setDeleteTarget(null);
      loadData();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Delete failed');
    }
  };

  if (loading) return <div className="page-loader">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h2 className="page-title">Lanes</h2>
        <button className="btn btn-primary" onClick={handleCreate}><FiPlus /> Add Route</button>
      </div>
      <DataTable columns={columns} data={routes} onEdit={handleEdit} onDelete={(row) => setDeleteTarget(row)} />
      {totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>
            <FiChevronLeft />
          </button>
          <span>Page {page + 1} of {totalPages} ({totalElements} total)</span>
          <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
            <FiChevronRight />
          </button>
        </div>
      )}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={editRoute ? 'Edit Route' : 'Add Route'}>
        <RouteForm route={editRoute} onSubmit={handleSubmit} onCancel={() => setModalOpen(false)} />
      </Modal>
      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete Route"
        message={`Delete route ${deleteTarget?.origin} → ${deleteTarget?.destination}?`}
      />
    </div>
  );
}

export default RouteList;

