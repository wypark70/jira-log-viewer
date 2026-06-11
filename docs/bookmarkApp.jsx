```react
import React, { useState, useEffect } from 'react';
import { Plus, Edit2, Trash2, MoreVertical, X, Link, Loader2 } from 'lucide-react';
import { HexColorPicker } from "react-colorful";
import { DndContext, closestCenter, useSensor, useSensors, TouchSensor, MouseSensor } from '@dnd-kit/core';
import { arrayMove, SortableContext, useSortable, rectSortingStrategy } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

// --- API 시뮬레이션 서비스 (10개 데이터) ---
const api = {
  get: () => new Promise(res => setTimeout(() => res([
    { id: '1', name: 'Google', url: 'https://google.com', bgColor: '#ffffff', textColor: '#374151' },
    { id: '2', name: 'NAVER', url: 'https://naver.com', bgColor: '#05c73a', textColor: '#ffffff' },
    { id: '3', name: 'GitHub', url: 'https://github.com', bgColor: '#24292e', textColor: '#ffffff' },
    { id: '4', name: 'YouTube', url: 'https://youtube.com', bgColor: '#ff0000', textColor: '#ffffff' },
    { id: '5', name: 'Notion', url: 'https://notion.so', bgColor: '#000000', textColor: '#ffffff' },
    { id: '6', name: 'Figma', url: 'https://figma.com', bgColor: '#5551ff', textColor: '#ffffff' },
    { id: '7', name: 'Slack', url: 'https://slack.com', bgColor: '#4a154b', textColor: '#ffffff' },
    { id: '8', name: 'Discord', url: 'https://discord.com', bgColor: '#5865f2', textColor: '#ffffff' },
    { id: '9', name: 'ChatGPT', url: 'https://chat.openai.com', bgColor: '#10a37f', textColor: '#ffffff' },
    { id: '10', name: 'Netflix', url: 'https://netflix.com', bgColor: '#e50914', textColor: '#ffffff' }
  ]), 800)),
  create: (item) => new Promise(res => setTimeout(() => res({ ...item, id: Date.now().toString() }), 600)),
  update: (item) => new Promise(res => setTimeout(() => res(item), 600)),
  delete: (id) => new Promise(res => setTimeout(() => res(true), 400))
};

const SortableBookmark = ({ bm, onEdit, onDelete, activeMenuId, setActiveMenuId }) => {
  const { attributes, listeners, setNodeRef, transform, transition } = useSortable({ id: bm.id });
  const style = { transform: CSS.Transform.toString(transform), transition };

  return (
    <div ref={setNodeRef} style={style} className="flex flex-col items-center relative group">
      <div className="w-20 h-20 sm:w-24 sm:h-24 rounded-3xl shadow-xl flex flex-col items-center justify-center font-bold text-2xl mb-3 cursor-grab active:cursor-grabbing transition hover:scale-105 hover:shadow-2xl ring-1 ring-black/5 relative overflow-hidden" 
           style={{ backgroundColor: bm.bgColor, color: bm.textColor }}
           {...attributes} {...listeners}>
        <button onClick={(e) => { e.stopPropagation(); setActiveMenuId(activeMenuId === bm.id ? null : bm.id); }} 
                className="absolute top-2 right-2 p-1 rounded-full hover:bg-black/10 transition z-10">
          <MoreVertical size={16} />
        </button>
        {bm.name.substring(0, 2)}
      </div>
      <span className="text-white font-semibold text-sm truncate max-w-[80px] drop-shadow-md">{bm.name}</span>

      {activeMenuId === bm.id && (
        <div className="absolute top-8 right-0 bg-white rounded-2xl shadow-2xl py-2 w-32 z-50 overflow-hidden ring-1 ring-black/5 animate-in fade-in zoom-in duration-200">
          <button onClick={() => { onEdit(bm); setActiveMenuId(null); }} className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-3 text-sm font-semibold">
            <Edit2 size={16} /> 수정
          </button>
          <button onClick={() => { onDelete(bm.id); setActiveMenuId(null); }} className="w-full text-left px-4 py-2 hover:bg-gray-50 flex items-center gap-3 text-sm font-semibold text-red-500">
            <Trash2 size={16} /> 삭제
          </button>
        </div>
      )}
    </div>
  );
};

export default function App() {
  const [bookmarks, setBookmarks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingItem, setEditingItem] = useState(null);
  const [activeMenuId, setActiveMenuId] = useState(null);
  const [colorMode, setColorMode] = useState('bg');
  const [preview, setPreview] = useState({ bgColor: '#ffffff', textColor: '#374151', name: '' });

  useEffect(() => {
    api.get().then(data => { setBookmarks(data); setLoading(false); });
  }, []);

  const handleSave = async (e) => {
    e.preventDefault();
    setLoading(true);
    const formData = new FormData(e.target);
    const item = { name: formData.get('name'), url: formData.get('url'), bgColor: preview.bgColor, textColor: preview.textColor };
    
    if (editingItem) {
      const updated = await api.update({ ...item, id: editingItem.id });
      setBookmarks(bookmarks.map(b => b.id === updated.id ? updated : b));
    } else {
      const created = await api.create(item);
      setBookmarks([...bookmarks, created]);
    }
    setIsModalOpen(false);
    setLoading(false);
  };

  const handleDelete = async (id) => {
    setLoading(true);
    await api.delete(id);
    setBookmarks(bookmarks.filter(b => b.id !== id));
    setLoading(false);
  };

  const sensors = useSensors(
    useSensor(MouseSensor, { activationConstraint: { distance: 5 } }),
    useSensor(TouchSensor, { activationConstraint: { delay: 200, tolerance: 5 } })
  );

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 p-6 sm:p-12" onClick={() => setActiveMenuId(null)}>
      {loading && (
        <div className="fixed inset-0 bg-black/20 z-50 flex items-center justify-center text-white">
          <Loader2 className="animate-spin" size={48} />
        </div>
      )}

      <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={(e) => {
        const { active, over } = e;
        if (over && active.id !== over.id) {
          setBookmarks((items) => {
            const oldIndex = items.findIndex((i) => i.id === active.id);
            const newIndex = items.findIndex((i) => i.id === over.id);
            return arrayMove(items, oldIndex, newIndex);
          });
        }
      }}>
        <div className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-6 lg:grid-cols-8 gap-x-6 gap-y-10 max-w-6xl mx-auto">
          <SortableContext items={bookmarks} strategy={rectSortingStrategy}>
            {bookmarks.map((bm) => (
              <SortableBookmark key={bm.id} bm={bm} activeMenuId={activeMenuId} setActiveMenuId={setActiveMenuId} 
                                onEdit={(item) => { setEditingItem(item); setPreview({bgColor: item.bgColor, textColor: item.textColor, name: item.name}); setIsModalOpen(true); }}
                                onDelete={handleDelete} />
            ))}
          </SortableContext>
          <button onClick={() => { setEditingItem(null); setPreview({bgColor: '#ffffff', textColor: '#374151', name: ''}); setIsModalOpen(true); }} className="flex flex-col items-center group">
            <div className="w-20 h-20 sm:w-24 sm:h-24 bg-white/10 rounded-3xl flex items-center justify-center text-white backdrop-blur-md border border-white/20 shadow-lg group-hover:bg-white/20 transition">
              <Plus size={40} />
            </div>
            <span className="text-white mt-3 font-semibold text-sm">추가</span>
          </button>
        </div>
      </DndContext>

      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <form onSubmit={handleSave} className="bg-white w-full max-w-sm rounded-[2rem] p-8 shadow-2xl animate-in zoom-in-95 duration-300">
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-extrabold text-gray-900">{editingItem ? '북마크 수정' : '새 북마크'}</h2>
              <button type="button" onClick={() => setIsModalOpen(false)} className="bg-gray-100 hover:bg-gray-200 p-2 rounded-full text-gray-600 transition"><X size={18}/></button>
            </div>
            
            <div className="flex flex-col gap-6 mb-6">
              <div className="flex items-center gap-4">
                <div className="w-20 h-20 rounded-3xl flex items-center justify-center font-bold text-3xl shadow-inner border border-black/5 shrink-0" 
                     style={{ backgroundColor: preview.bgColor, color: preview.textColor }}>
                  {preview.name.substring(0, 1) || 'A'}
                </div>
                <div className="flex-1 space-y-2">
                  <input name="name" onChange={(e) => setPreview({...preview, name: e.target.value})} defaultValue={editingItem?.name} placeholder="북마크 이름" className="w-full px-4 py-3 bg-gray-50 rounded-2xl outline-none font-bold text-sm" required />
                  <div className="flex items-center px-4 bg-gray-50 rounded-2xl">
                    <Link size={16} className="text-gray-400 mr-2" />
                    <input name="url" defaultValue={editingItem?.url} placeholder="URL 주소" className="w-full py-3 bg-transparent outline-none text-sm" required />
                  </div>
                </div>
              </div>
            </div>

            <div className="bg-gray-50 p-4 rounded-3xl mb-6">
              <div className="flex p-1 bg-gray-200/50 rounded-2xl mb-4">
                <button type="button" onClick={() => setColorMode('bg')} className={`flex-1 py-2 rounded-xl text-xs font-bold transition ${colorMode === 'bg' ? 'bg-white shadow-sm' : 'text-gray-500'}`}>배경 색상</button>
                <button type="button" onClick={() => setColorMode('text')} className={`flex-1 py-2 rounded-xl text-xs font-bold transition ${colorMode === 'text' ? 'bg-white shadow-sm' : 'text-gray-500'}`}>글자 색상</button>
              </div>
              <div className="flex justify-center p-2">
                <HexColorPicker 
                  color={colorMode === 'bg' ? preview.bgColor : preview.textColor} 
                  onChange={(color) => colorMode === 'bg' ? setPreview({...preview, bgColor: color}) : setPreview({...preview, textColor: color})} 
                />
              </div>
            </div>

            <button type="submit" className="w-full py-4 bg-indigo-600 text-white rounded-2xl font-bold hover:bg-indigo-700 transition active:scale-95">
              저장하기
            </button>
          </form>
        </div>
      )}
    </div>
  );
}


```
